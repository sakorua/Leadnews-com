package com.heima.article.listen;

import com.heima.article.service.ApArticleService;
import com.heima.common.constants.message.PublishArticleConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mrchen
 * @date 2022/3/5 11:43
 */
@Component
@Slf4j
public class PublishArticleListener {
    @Autowired
    ApArticleService apArticleService;
    @RabbitListener(queuesToDeclare = @Queue(PublishArticleConstants.PUBLISH_ARTICLE_QUEUE))
    public void handlePublishMsg(String newsId){
        log.info("接收到发布文章的消息  文章id:{}",newsId);
        try {
            apArticleService.publishArticle(Integer.valueOf(newsId));
            log.info("发布文章成功  文章id:{}",newsId);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            log.error("发布文章失败  文章id:{}  原因: {}",newsId,e.getMessage());
        }
    }
}
