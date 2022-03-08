package com.heima.article.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.mapper.AuthorMapper;
import com.heima.article.service.ApArticleService;
import com.heima.common.constants.article.ArticleConstants;
import com.heima.common.exception.CustException;
import com.heima.feigns.AdminFeign;
import com.heima.feigns.WemediaFeign;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.article.dtos.ArticleHomeDTO;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmNews;
import io.seata.common.util.StringUtils;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {


    @Autowired
    private ApArticleMapper apArticleMapper;

    @Value("${file.oss.web-site}")
    private String webSite;

    @Value("${file.minio.readPath}")
    String readPath;


    @GlobalTransactional(rollbackFor = Exception.class, timeoutMills = 100000)
    @Override
    public void publishArticle(Integer newsId) {
        // 1. 查询 并 校验自媒体文章
        WmNews wmNews = getWmNews(newsId);
        // 2. 封装成apArticle
        ApArticle article = getApArticle(wmNews);
        // 3. 保证或修改  apArticle文章信息
        saveOrUpdateArticle(article);
        // 4. 保存 配置 config  和 内容 content 表信息
        saveConfigAndContent(article, wmNews);
        // 5. TODO 页面静态化
        // 6. 更新 wmNews状态
        updateWmNews(wmNews, article);
        // 7. TODO 通知es索引库新增文章索引
    }

    @Override
    public ResponseResult load(Short loadtype, ArticleHomeDTO dto) {
        //  检查页码
        Integer size = dto.getSize();
        if (size == null || size < 0) {
            size = 10;
        }
        dto.setSize(size);

        //  检查频道
        if (StringUtils.isBlank(dto.getTag())) {
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }

        //  检查时间
        if (dto.getMaxBehotTime() == null) {
            dto.setMaxBehotTime(new Date());
        }
        if (dto.getMinBehotTime() == null) {
            dto.setMinBehotTime(new Date());
        }

        //  检查类型
        if ((!loadtype.equals(ArticleConstants.LOADTYPE_LOAD_MORE)) && (!loadtype.equals(ArticleConstants.LOADTYPE_LOAD_NEW))) {
            loadtype = ArticleConstants.LOADTYPE_LOAD_MORE;
        }

        List<ApArticle> articleList = apArticleMapper.loadArticleList(dto, loadtype);
        for (ApArticle article : articleList) {
            String images = article.getImages();
            if (StringUtils.isNotBlank(images)) {
                images = Arrays.stream(images.split(",")).map(url -> webSite + url).collect(Collectors.joining(","));
            }


            article.setStaticUrl(readPath + article.getStaticUrl());


        }
        return ResponseResult.okResult(articleList);
    }

    private void updateWmNews(WmNews wmNews, ApArticle article) {
        wmNews.setStatus(WmNews.Status.PUBLISHED.getCode());
        wmNews.setArticleId(article.getId());
        ResponseResult result = wemediaFeign.updateWmNews(wmNews);
        if (!result.checkCode()) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
    }

    /**
     * 保存 config表  content表
     *
     * @param article
     * @param wmNews
     */
    private void saveConfigAndContent(ApArticle article, WmNews wmNews) {
        ApArticleConfig config = new ApArticleConfig();
        config.setArticleId(article.getId());
        config.setIsComment(true);
        config.setIsForward(true);
        config.setIsDown(false);
        config.setIsDelete(false);
        apArticleConfigMapper.insert(config);
        ApArticleContent content = new ApArticleContent();
        content.setArticleId(article.getId());
        content.setContent(wmNews.getContent());
        apArticleContentMapper.insert(content);
    }

    @Autowired
    ApArticleConfigMapper apArticleConfigMapper;

    @Autowired
    ApArticleContentMapper apArticleContentMapper;

    /**
     * 保存或修改文章
     *
     * @param article
     */
    private void saveOrUpdateArticle(ApArticle article) {
        // 1. 判断 id是否存在
        if (article.getId() == null) {
            // 2. 不存在  新增article 并补全信息
            article.setCollection(0);
            article.setComment(0);
            article.setLikes(0);
            article.setViews(0);
            save(article);
        } else {
            // 3. 存在， 判断id关联的对象是否存在, 删除关联的config content
            ApArticle byId = getById(article.getId());
            if (byId == null) {
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "关联的文章数据不存在");
            }
            updateById(article);
            apArticleConfigMapper.delete(Wrappers.<ApArticleConfig>lambdaQuery().eq(ApArticleConfig::getArticleId, article.getId()));
            apArticleContentMapper.delete(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, article.getId()));
        }
    }


    @Autowired
    AdminFeign adminFeign;

    @Autowired
    AuthorMapper authorMapper;

    /**
     * 封装article对象
     *
     * @param wmNews
     * @return
     */
    private ApArticle getApArticle(WmNews wmNews) {
        // 1. 创建对象
        ApArticle article = new ApArticle();
        BeanUtils.copyProperties(wmNews, article);
        // 2. 设置id  flag(0)  layout布局
        article.setId(wmNews.getArticleId());
        article.setFlag((byte) 0);
        article.setLayout(wmNews.getType());
        // 3. 设置频道名称   feign ==> adminFeign
        ResponseResult<AdChannel> channelResult = adminFeign.findOne(wmNews.getChannelId());
        if (!channelResult.checkCode()) {
            log.error("文章发布失败  原因: 远程调用频道查询失败 ");
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        AdChannel channel = channelResult.getData();
        if (channel == null) {
            log.error("文章发布失败  原因: 频道信息不存在 ");
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        article.setChannelName(channel.getName());
        // 4. 设置作者信息  根据 wm_user_id ==> ap_author表中
        ApAuthor apAuthor = authorMapper.selectOne(Wrappers.<ApAuthor>lambdaQuery().eq(ApAuthor::getWmUserId, wmNews.getUserId()));
        if (apAuthor == null) {
            log.error("文章发布失败  原因: 关联的作者信息不存在 ");
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        article.setAuthorId(Long.valueOf(apAuthor.getId()));
        article.setAuthorName(apAuthor.getName());
        // 5. 返回结果
        return article;
    }

    @Autowired
    WemediaFeign wemediaFeign;

    private WmNews getWmNews(Integer newsId) {
        ResponseResult<WmNews> wmNewsResult = wemediaFeign.findWmNewsById(newsId);
        if (!wmNewsResult.checkCode()) {
            log.error("文章发布失败 远程调用自媒体文章接口失败  文章id: {}", newsId);
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        WmNews wmNews = wmNewsResult.getData();
        if (wmNews == null) {
            log.error("文章发布失败 远程调用自媒体文章接口失败  文章id: {}", newsId);
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        short status = wmNews.getStatus().shortValue();
        if (WmNews.Status.ADMIN_SUCCESS.getCode() != status
                && WmNews.Status.SUCCESS.getCode() != status) {
            log.error("文章发布失败 自媒体文章状态错误  文章id: {}   文章状态:{}", newsId, wmNews.getStatus());
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "文章状态 不为 4或8  无法发布");
        }
        return wmNews;
    }
}