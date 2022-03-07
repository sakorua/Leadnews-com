package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.wemedia.NewsAutoScanConstants;
import com.heima.common.constants.wemedia.WemediaConstants;
import com.heima.common.exception.CustException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.WmThreadLocalUtils;
import com.heima.model.wemedia.dtos.NewsAuthDTO;
import com.heima.model.wemedia.dtos.WmNewsDTO;
import com.heima.model.wemedia.dtos.WmNewsPageReqDTO;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.model.wemedia.vos.WmNewsVO;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@SuppressWarnings("all")
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    /**
     * 注入网页前缀
     */
    @Value("${file.oss.web-site}")
    String webSite;

    @Autowired
    WmNewsMaterialMapper wmNewsMaterialMapper;

    @Autowired
    WmMaterialMapper wmMaterialMapper;

    /**
     * 分页查找文章
     *
     * @param dto 前端dto参数
     * @return 返回结果
     */
    @Override
    public ResponseResult findList(WmNewsPageReqDTO dto) {
        // 1. 校验参数   分页   是否登陆
        dto.checkParam();
        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        // 2. 封装查询条件
        LambdaQueryWrapper<WmNews> queryWrapper = Wrappers.<WmNews>lambdaQuery();
        // 2.1  分页条件
        Page<WmNews> pageReq = new Page<>(dto.getPage(), dto.getSize());
        // 2.2  查询条件
        //  标题
        String keyword = dto.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.like(WmNews::getTitle, keyword);
        }
        //  频道id
        Integer channelId = dto.getChannelId();
        if (channelId != null) {
            queryWrapper.eq(WmNews::getChannelId, channelId);
        }
        //  状态
        Short status = dto.getStatus();
        if (status != null) {
            queryWrapper.eq(WmNews::getStatus, status);
        }
        //  开始时间 和 结束时间
        Date beginPubDate = dto.getBeginPubDate();
        if (beginPubDate != null) {
            queryWrapper.ge(WmNews::getPublishTime, beginPubDate);
        }
        Date endPubDate = dto.getEndPubDate();
        if (endPubDate != null) {
            queryWrapper.le(WmNews::getPublishTime, endPubDate);
        }
        //  登陆用户id
        queryWrapper.eq(WmNews::getUserId, user.getId());
        //  创建时间  降序
        queryWrapper.orderByDesc(WmNews::getCreatedTime);
        // 3. 执行查询 返回结果   设置:host (webSite)
        IPage<WmNews> pageResult = this.page(pageReq, queryWrapper);
        PageResponseResult result = new PageResponseResult(dto.getPage(), dto.getSize(), pageResult.getTotal(), pageResult.getRecords());
        result.setHost(webSite);
        return result;
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发布文章
     *
     * @param dto 前端传入参数
     * @return 返回参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult submitNews(WmNewsDTO dto) {
        //校验参数 dto(标题  content )  是否登陆
        if (StringUtils.isBlank(dto.getTitle()) || StringUtils.isBlank(dto.getContent())) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "标题或内容不能为空");
        }
        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        //wmNews
        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(dto, wmNews);
        //type -1
        if (WemediaConstants.WM_NEWS_TYPE_AUTO.equals(dto.getType())) {
            // -1 报错 设置为null
            wmNews.setType(null);
        }


        //images 前端集合
        wmNews.setImages(imagesToStr(dto.getImages()));
        wmNews.setUserId(user.getId());
        //根据id修改或者报错文章
        saveOrUpdateWmNews(wmNews);
        if (WemediaConstants.WM_NEWS_DRAFT_STATUS.equals(dto.getStatus())) {
            return ResponseResult.okResult();
        }

        //保存文章内容 封面  和素材的关联关系
        //抽取出内容中引用的素材url路径
        List<String> materialUrlList = parseContentImages(dto.getContent());

        //保存内容和素材的关联关系
        if (!CollectionUtils.isEmpty(materialUrlList)) {
            saveRelativeInfoForContent(materialUrlList, wmNews);
        }
        //保存封面和素材的关联关系 如果type == -1 自动生成素材
        saveRelativeInfoForCover(materialUrlList, wmNews, dto);


        //发送审核消息
        rabbitTemplate.convertAndSend(NewsAutoScanConstants.WM_NEWS_AUTO_SCAN_QUEUE, wmNews.getId());
        log.info("成功发送 待审核消息 ==> 队列:{}, 文章id:{}", NewsAutoScanConstants.WM_NEWS_AUTO_SCAN_QUEUE, wmNews.getId());

        return ResponseResult.okResult();
    }

    /**
     * 根据id查询文章
     *
     * @param id id
     * @return 返回值
     */
    @Override
    public ResponseResult findById(Integer id) {
        //判断参数
        if (id == null) {
            log.error("根据id查询文章时，id为null");
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews one = this.getOne(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getId, id));
        return ResponseResult.okResult(one);
    }

    /**
     * 根据id删除文章
     *
     * @param id id
     * @return 返回状态
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult deleteById(Integer id) {
        //判断参数
        if (id == null) {
            log.error("根据id删除文章时，id为null");
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = getById(id);
        //判断当前文章的状态  status==9  enable == 1
        if (wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())
                && wmNews.getEnable().equals(WemediaConstants.WM_NEWS_UP)) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "文章已发布，不能删除");
        }
        //先根据id查询中间表id 删除中间表
        int delete = wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, id));
        if (delete <= 0) {
            log.error("根据id删除文章素材中间表时报错，没有得到数据");
        }
        //再删除news表
        boolean removeById = this.removeById(id);
        if (!removeById) {
            log.error("根据id删除文章表时报错，没有得到数据");
        }
        return ResponseResult.okResult();
    }

    /**
     * 文章上下架操作
     *
     * @param dto {
     *            enable:0
     *            id : 1
     *            }
     * @return 返回状态
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult downOrUpNews(WmNewsDTO dto) {
        Short enable = dto.getEnable();
        Integer id = dto.getId();
        //判断参数
        if (dto == null || id == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //是否上架  0 下架  1 上架
        if (enable == null || (!WemediaConstants.WM_NEWS_UP.equals(enable) && !WemediaConstants.WM_NEWS_DOWN.equals(enable))) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "上下架状态错误");
        }
        //查询文章
        WmNews wmNews = this.getOne(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getId, id));
        if (wmNews == null) {
            log.error("上下架文章查询文章为null");
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在");
        }
        //判断文章是否发布
        if (!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "当前文章不是发布状态，不能上下架");
        }
        //修改文章状态，同步到app端（后期做）TODO
        update(Wrappers.<WmNews>lambdaUpdate().eq(WmNews::getId, dto.getId())
                .set(WmNews::getEnable, dto.getEnable()));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

    }

    @Autowired
    private WmNewsMapper wmNewsMapper;

    /**
     * 查询文章列表
     *
     * @param dto 前端请求
     * @return 返回
     */
    @Override
    public ResponseResult findAllNewsList(NewsAuthDTO dto) {
        //检查参数
        dto.checkParam();
        //页码
        Integer page = dto.getPage();
        /*
				公式： 起始索引  = （当前页码-1）*每页显示的条数
				    	0			1				2
				    	2			2				2
				    	4			3				2
		 */
        //起始索引
        dto.setPage((dto.getPage() - 1) * dto.getSize());
        if (StringUtils.isNotBlank(dto.getTitle())) {
            dto.setTitle("%" + dto.getTitle() + "%");
        }

        //分页查询
        List<WmNewsVO> wmNewsVoList = wmNewsMapper.findListAndPage(dto);

        long count = wmNewsMapper.findListCount(dto);
        //结果返回
        ResponseResult result = new PageResponseResult(page, dto.getSize(), count, wmNewsVoList);
        result.setHost(webSite);
        return result;
    }

    @Autowired
    private WmUserMapper wmUserMapper;

    /**
     * 查询文章详情
     *
     * @param id 文章id
     * @return 返回vo对象
     */
    @Override
    public ResponseResult findWmNewsVo(Integer id) {
        //1参数检查
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.查询文章信息
        WmNews wmNews = getById(id);
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //3.查询作者
        WmUser wmUser = null;
        if (wmNews.getUserId() != null) {
            wmUser = wmUserMapper.selectById(wmNews.getUserId());
        }

        //4.封装vo信息返回
        WmNewsVO wmNewsVo = new WmNewsVO();
        BeanUtils.copyProperties(wmNews, wmNewsVo);
        if (wmUser != null) {
            wmNewsVo.setAuthorName(wmUser.getName());
        }
        ResponseResult responseResult = ResponseResult.okResult(wmNewsVo);
        responseResult.setHost(webSite);
        return responseResult;
    }

    /**
     * 自媒体文章人工审核
     * @param status 2  审核失败  4 审核成功
     * @param dto
     * @return
     */
    @Override
    public ResponseResult updateStatus(Short status, NewsAuthDTO dto) {
        //1.参数检查
        if(dto == null || dto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.查询文章
        WmNews wmNews = getById(dto.getId());
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        // 检查文章状态 不能为9  已发布
        if (wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW,"文章已发布");
        }
        //3.修改文章状态
        wmNews.setStatus(status);
        if(StringUtils.isNotBlank(dto.getMsg())){
            wmNews.setReason(dto.getMsg());
        }
        updateById(wmNews);

        // TODO 通知定时发布文章

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }



    /**
     * 保存 封面和素材的关联关系
     *
     * @param materialUrlList 素材地址集合
     * @param wmNews          文章pojo
     */
    private void saveRelativeInfoForCover(List<String> materialUrlList, WmNews wmNews, WmNewsDTO dto) {
        // 原本的封面集合列表
        List<String> images = dto.getImages();
        // 判断类型是否为-1
        if (WemediaConstants.WM_NEWS_TYPE_AUTO.equals(dto.getType())) {
            // 需要自动生成封面
            int size = materialUrlList.size();
            // 如果 size 数量大于0 小于等于2  设置为单图  ， 截取素材url集合中的 1个作为封面
            if (size > 0 && size <= 2) {
                images = materialUrlList.stream().limit(1).collect(Collectors.toList());
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
            } else if (size > 2) {
                // 如果 size 数量大于2  设置为多图  ， 截取素材url集合中的 3个作为封面
                images = materialUrlList.stream().limit(3).collect(Collectors.toList());
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
            } else {
                // 设置为无图
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }
            // 如果有封面图 将图片集合转为字符串设置到wmNews.images 属性中
            if (!CollectionUtils.isEmpty(images)) {
                wmNews.setImages(imagesToStr(images));
            }
            // 修改wmNews
            updateById(wmNews);
        }
        // 如果不为空 保存关联关系
        if (!CollectionUtils.isEmpty(images)) {
            images = images.stream().map(url -> url.replaceAll(webSite, "")).collect(Collectors.toList());
            saveRelativeInfo(images, wmNews, WemediaConstants.WM_IMAGE_REFERENCE);
        }
    }

    /**
     * 保存   文章内容涉及的图片 和 素材的关联关系
     *
     * @param materialUrlList 素材地址集合
     * @param wmNews          文章pojo
     */
    private void saveRelativeInfoForContent(List<String> materialUrlList, WmNews wmNews) {
        saveRelativeInfo(materialUrlList, wmNews, WemediaConstants.WM_CONTENT_REFERENCE);
    }

    /**
     * 保存关联关系
     *
     * @param materialUrlList 素材地址集合
     * @param wmNews          文章pojo
     * @param type            自定义类型
     */
    private void saveRelativeInfo(List<String> materialUrlList, WmNews wmNews, Short type) {
        // 根据url路径列表 查询对应的id
        List<Integer> ids = wmMaterialMapper.selectRelationsIds(materialUrlList, WmThreadLocalUtils.getUser().getId());
        // 判断是否缺失素材
        if (CollectionUtils.isEmpty(ids) || ids.size() < materialUrlList.size()) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "引用的素材不存在");
        }
        wmNewsMaterialMapper.saveRelations(ids, wmNews.getId(), type);
    }

    /**
     * [{type:text,value:文本},{type:image,value:url},{type:image,value:url},{type:image,value:url},{type:image,value:url}]
     * 抽取出 内容中涉及的 素材路径
     *
     * @param content 前端传送文本信息
     */
    private List<String> parseContentImages(String content) {
        List<Map> contentMap = JSON.parseArray(content, Map.class);
        // 创建流
        return contentMap.stream()
                // 过滤type为image的数据
                .filter(m -> WemediaConstants.WM_NEWS_TYPE_IMAGE.equals(m.get("type")))
                // 提取每个map中value的值 ==> url路径
                .map(m -> (String) m.get("value"))
                // 替换前缀路径
                .map(url -> url.replaceAll(webSite, ""))
                // 去重
                .distinct()
                // 收集到集合中
                .collect(Collectors.toList());
    }

    /**
     * 保存或修改文章信息
     *
     * @param wmNews pojo
     */
    private void saveOrUpdateWmNews(WmNews wmNews) {
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        // 设置上架
        wmNews.setEnable(WemediaConstants.WM_NEWS_UP);
        if (wmNews.getId() == null) {
            // 保存
            save(wmNews);
        } else {
            // 修改
            wmNewsMaterialMapper.delete(
                    Wrappers.<WmNewsMaterial>lambdaQuery()
                            .eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            updateById(wmNews);
        }
    }

    /**
     * 将图片集合  转为  字符串  按照逗号拼接
     *
     * @param images [webSite+url1,webSite+url2,webSite+url3]
     * @return 结果
     */
    private String imagesToStr(List<String> images) {
        if (!CollectionUtils.isEmpty(images)) {
            return images.stream()
                    // 去除前缀路径
                    .map(url -> url.replaceAll(webSite, ""))
                    .collect(Collectors.joining(","));
        }
        return null;
    }
}