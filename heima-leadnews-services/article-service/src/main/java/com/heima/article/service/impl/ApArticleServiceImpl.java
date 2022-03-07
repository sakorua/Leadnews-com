package com.heima.article.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.mapper.AuthorMapper;
import com.heima.article.service.ApArticleService;
import com.heima.common.exception.CustException;
import com.heima.feigns.admin.AdminFeign;
import com.heima.feigns.wemedia.WemediaFeign;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmNews;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {

    @Autowired
    private WemediaFeign wemediaFeign;
    @Autowired
    private AdminFeign adminFeign;
    @Autowired
    private AuthorMapper authorMapper;
    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;
    @Autowired
    private ApArticleContentMapper apArticleContentMapper;


    @GlobalTransactional(rollbackFor = Exception.class, timeoutMills = 100000)
    @Override
    public void publishArticle(Integer newsId) {
        //查询并检查自媒体文章
        WmNews wmNews = getWmNews(newsId);
        //封装ApArticle

        ApArticle apArticle = getApArticle(wmNews);

        //保存或修改 article信息
        saveOrUpdateArticle(apArticle);
        //保存关联配置和内容信息
        saveConfigAndContent(wmNews, apArticle);
        //TODO 文章页面静态

        //更新wmNews状态 改为9 并设置articleId
        updateWmNews(newsId, wmNews, apArticle);
        //TODO 通知es索引库添加文章索引
    }

    private void updateWmNews(Integer newsId, WmNews wmNews, ApArticle apArticle) {
        wmNews.setStatus(WmNews.Status.PUBLISHED.getCode());
        wmNews.setArticleId(apArticle.getId());
        ResponseResult updateResult = wemediaFeign.updateWmNews(wmNews);
        if (!updateResult.checkCode()) {
            log.error("文章发布失败 远程调用修改文章接口失败， 不予发布");
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,"远程调用修改文章接口失败");
        }
    }

    private void saveConfigAndContent(WmNews wmNews, ApArticle apArticle) {
        // 添加配置信息
        ApArticleConfig apArticleConfig = new ApArticleConfig();
        apArticleConfig.setArticleId(apArticle.getId());
        apArticleConfig.setIsComment(true);
        apArticleConfig.setIsForward(true);
        apArticleConfig.setIsDown(false);
        apArticleConfig.setIsDelete(false);
        apArticleConfigMapper.insert(apArticleConfig);
        // 添加文章详情
        ApArticleContent apArticleContent = new ApArticleContent();
        apArticleContent.setArticleId(apArticle.getId());
        apArticleContent.setContent(wmNews.getContent());
        apArticleContentMapper.insert(apArticleContent);
    }

    private void saveOrUpdateArticle(ApArticle apArticle) {
        // 判断wmNews之前是否关联 articleId
        if(apArticle.getId() == null){
            // 无关联  新增 article
            // 保存文章
            //   收藏数
            apArticle.setCollection(0);
            //   点赞数
            apArticle.setLikes(0);
            //   评论数
            apArticle.setComment(0);
            //   阅读数
            apArticle.setViews(0);
            save(apArticle);
        }else {
            // 有关联  修改 article
            // 修改文章  删除之前关联的配置信息   内容信息
            ApArticle article = getById(apArticle.getId());
            if(article == null){
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"关联的文章不存在");
            }
            updateById(apArticle);
            apArticleConfigMapper.delete(Wrappers.<ApArticleConfig>lambdaQuery().eq(ApArticleConfig::getArticleId,apArticle.getId()));
            apArticleContentMapper.delete(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId,apArticle.getId()));
        }
    }

    private ApArticle getApArticle(WmNews wmNews) {
        ApArticle apArticle = new ApArticle();

        BeanUtils.copyProperties(wmNews, apArticle);
        apArticle.setId(wmNews.getArticleId());
        apArticle.setFlag((byte) 0);
        apArticle.setLayout(wmNews.getType());

        //远程查询频道信息
        ResponseResult<AdChannel> channelResult = adminFeign.findOne(wmNews.getChannelId());
        if (!channelResult.checkCode()) {
            log.error("文章发布失败 远程调用查询频道出现异常， 不予发布");
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR, "远程调用查询频道出现异常");
        }
        AdChannel channel = channelResult.getData();
        if (channel == null) {
            log.error("文章发布失败 未查询到相关频道信息， 不予发布");
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "未查询到相关频道信息");
        }

        apArticle.setChannelName(channel.getName());
        ApAuthor author = authorMapper.selectOne(Wrappers.<ApAuthor>lambdaQuery().eq(ApAuthor::getWmUserId, wmNews.getUserId()));
        if (author == null) {
            log.error("文章发布失败 未查询到相关作者信息， 不予发布");
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "根据自媒体用户，查询关联作者信息失败");
        }
        apArticle.setAuthorId(Long.valueOf(author.getId()));
        apArticle.setAuthorName(author.getName());
        return apArticle;
    }


    private WmNews getWmNews(Integer newsId) {
        ResponseResult<WmNews> newsResult = wemediaFeign.findWmNewsById(newsId);
        if (!newsResult.checkCode()) {
            log.error("文章发布失败 远程调用自媒体文章接口失败");
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR, "远程调用自媒体文章接口失败");
        }
        WmNews wmNews = newsResult.getData();
        if (wmNews == null) {
            log.error("文章发布失败 未获取到自媒体文章信息");
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "未查询到自媒体文章");
        }
        short status = wmNews.getStatus().shortValue();
        if (status != WmNews.Status.ADMIN_SUCCESS.getCode() && status != WmNews.Status.SUCCESS.getCode()) {
            log.error("文章发布失败 文章状态不为 4 或 8， 不予发布");
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "自媒体文章状态错误");
        }
        return wmNews;
    }
}