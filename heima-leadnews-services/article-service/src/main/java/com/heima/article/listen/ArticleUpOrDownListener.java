package com.heima.article.listen;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.service.ApArticleConfigService;
import com.heima.common.constants.message.NewsUpOrDownConstants;
import com.heima.model.article.pojos.ApArticleConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mrchen
 * @date 2022/3/5 14:40
 */
@Component
@Slf4j
public class ArticleUpOrDownListener {

    @Autowired
    ApArticleConfigService apArticleConfigService;

    @RabbitListener(queuesToDeclare = @Queue(NewsUpOrDownConstants.NEWS_UP_FOR_ARTICLE_CONFIG_QUEUE))
    public void handleNewsUp(String articleId){
        log.info("接收到  文章上架消息  文章id:{}",articleId);
        // 根据articleId 作为条件 将 isDown 修改为false
        apArticleConfigService.update(
                Wrappers.<ApArticleConfig>lambdaUpdate()
                                        .eq(ApArticleConfig::getArticleId,articleId)
                                        .set(ApArticleConfig::getIsDown,false)
        );
    }
    @RabbitListener(queuesToDeclare = @Queue(NewsUpOrDownConstants.NEWS_DOWN_FOR_ARTICLE_CONFIG_QUEUE))
    public void handleNewsDown(String articleId){
        log.info("接收到  文章下架消息  文章id:{}",articleId);
        // 根据articleId 作为条件 将 isDown 修改为false
        apArticleConfigService.update(
                Wrappers.<ApArticleConfig>lambdaUpdate()
                        .eq(ApArticleConfig::getArticleId,articleId)
                        .set(ApArticleConfig::getIsDown,true)
        );
    }
}
