package com.heima.wemedia.serivce.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustException;
import com.heima.model.common.constants.wemedia.WemediaConstants.WemediaConstants;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.media.dtos.WmNewsDTO;
import com.heima.model.threadlocal.WmThreadLocalUtils;
import com.heima.model.wemedia.dtos.WmNewsPageReqDTO;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.serivce.WmNewsService;
import io.jsonwebtoken.lang.Collections;
import io.seata.common.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SaKoRua
 * @date 2022-03-03 9:31 AM
 * @Description //TODO
 */
@Service
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Value("${file.oss.web-site}")
    private String webSite;

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;
    @Autowired
    private WmMaterialMapper wmMaterialMapper;

    @Override
    public ResponseResult findList(WmNewsPageReqDTO dto) {

        //参数检查
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();
        //条件封装执行查询

        LambdaQueryWrapper<WmNews> wrapper = Wrappers.lambdaQuery();

        //文章标题模糊查询
        if (StringUtils.isNotBlank(dto.getKeyword())) {
            wrapper.like(WmNews::getTitle, dto.getKeyword());
        }
        //频道id
        if (dto.getChannelId() != null) {
            wrapper.eq(WmNews::getChannelId, dto.getChannelId());
        }
        //文章状态
        if (dto.getStatus() != null) {
            wrapper.eq(WmNews::getStatus, dto.getStatus());
        }

        //发布时间
        if (dto.getBeginPubDate() != null) {
            wrapper.ge(WmNews::getPublishTime, dto.getBeginPubDate());
        }
        if (dto.getEndPubDate() != null) {
            wrapper.le(WmNews::getPublishTime, dto.getEndPubDate());
        }

        //当前用户文章
        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }

        wrapper.eq(WmNews::getUserId, user.getId());
        //按照创建日期排序

        wrapper.orderByDesc(WmNews::getCreatedTime);
        //分页配置

        Page<WmNews> wmNewsPage = new Page<>(dto.getPage(), dto.getSize());
        IPage<WmNews> page = this.page(wmNewsPage, wrapper);

        PageResponseResult result = new PageResponseResult(dto.getPage(), dto.getSize(), page.getTotal());
        result.setData(page.getRecords());

        result.setHost(webSite);
        return result;
    }


    @Override
    public ResponseResult submitNews(WmNewsDTO dto) {

        if (StringUtils.isBlank(dto.getTitle()) || StringUtils.isBlank(dto.getTitle())) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(dto, wmNews);

        //文章布局设置 如果布局为自动 则设置为null
        if (WemediaConstants.WM_NEWS_TYPE_AUTO.equals(dto.getType())) {
            wmNews.setType(null);
        }

        // 处理dto参数 images封面集合 转换成 字符串
        String images = imageListToStr(dto.getImages());
        wmNews.setImages(images);
        wmNews.setUserId(user.getId());


        saveWmNews(wmNews);


        // 如果是草稿  直接返回
        if (WemediaConstants.WM_NEWS_DRAFT_STATUS.equals(dto.getStatus())) {
            return ResponseResult.okResult();
        }

        //TODO:
        List<String> materialUrlList = parseContentImages(dto.getContent());
        // 3.2 保存内容和素材的关联关系
        if (!CollectionUtils.isEmpty(materialUrlList)) {
            saveRelativeInfo(materialUrlList, wmNews, WemediaConstants.WM_IMAGE_REFERENCE);
        }
        // 3.3 保存封面和素材的关联关系
        saveRelativeInfoForCover(materialUrlList, wmNews, dto);
        //           如果type == -1 需要根据内容素材列表生成封面图片
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult findWmNewsById(Integer id) {
        //1 参数检查
        if (id == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2 执行查询
        WmNews wmNews = getById(id);
        if (wmNews == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //3 返回结果
        ResponseResult result = ResponseResult.okResult(wmNews);
        result.setHost(webSite);
        return result;
    }

    @Override
    public ResponseResult delNews(Integer id) {
        //1.检查参数
        if (id == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "文章Id不可缺少");
        }
        //2.获取数据
        WmNews wmNews = getById(id);
        if (wmNews == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在");
        }
        //3.判断当前文章的状态  status==9  enable == 1
        if (wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())
                && wmNews.getEnable().equals(WemediaConstants.WM_NEWS_UP)) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "文章已发布，不能删除");
        }
        //4.去除素材与文章的关系
        wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));
        //5.删除文章
        removeById(wmNews.getId());
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult downOrUp(WmNewsDTO dto) {
        //1.检查参数
        if(dto == null || dto.getId() == null){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        Short enable = dto.getEnable();
        if(enable == null ||
                (!WemediaConstants.WM_NEWS_UP.equals(enable)&&!WemediaConstants.WM_NEWS_DOWN.equals(enable))){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"上下架状态错误");
        }
        //2.查询文章
        WmNews wmNews = getById(dto.getId());
        if(wmNews == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"文章不存在");
        }
        //3.判断文章是否发布
        if(!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"当前文章不是发布状态，不能上下架");
        }
        //4.修改文章状态，同步到app端（后期做）TODO
        update(Wrappers.<WmNews>lambdaUpdate().eq(WmNews::getId,dto.getId())
                .set(WmNews::getEnable,dto.getEnable()));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

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

    private void saveRelativeInfo(List<String> materialUrlList, WmNews wmNews, Short type) {
        //1 查询文章内容中的图片对应的素材ID
        List<Integer> ids = wmMaterialMapper.selectRelationsIds(materialUrlList,
                WmThreadLocalUtils.getUser().getId());
        //2 判断素材是否缺失
        if (CollectionUtils.isEmpty(ids) || ids.size() < materialUrlList.size()) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "相关素材缺失,保存文章失败");
        }
        //3 保存素材关系
        wmNewsMaterialMapper.saveRelations(ids, wmNews.getId(), type);
    }

    private List<String> parseContentImages(String content) {
        List<Map> contents = JSON.parseArray(content, Map.class);
        return contents.stream().filter(map -> map.get("type").equals(WemediaConstants.WM_NEWS_TYPE_IMAGE))
                .map(x -> (String) x.get("value"))
                .map(url -> url.replace(webSite, "")
                        .replace(" ", ""))
                .distinct()
                .collect(Collectors.toList());
    }

    private void saveWmNews(WmNews wmNews) {
        wmNews.setCreatedTime(new Date());
        wmNews.setUserId(WmThreadLocalUtils.getUser().getId());
        wmNews.setSubmitedTime(new Date());
        //  上架
        wmNews.setEnable(WemediaConstants.WM_NEWS_UP);
        //  保存
        if (wmNews.getId() == null) {
            save(wmNews);
        } else {
            // 当前文章 和 素材关系表数据删除
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery()
                    .eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            updateById(wmNews);
        }
    }

    private String imagesToStr(List<String> images) {
        if (!CollectionUtils.isEmpty(images)) {
            return images.stream()
                    // 去除前缀路径
                    .map(url -> url.replaceAll(webSite, ""))
                    .collect(Collectors.joining(","));
        }
        return null;
    }

    private String imageListToStr(List<String> images) {
        return images.stream().map(url -> url.replace(webSite, "")).collect(Collectors.joining(","));
    }
}
