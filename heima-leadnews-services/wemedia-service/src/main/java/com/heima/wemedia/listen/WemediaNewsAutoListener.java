package com.heima.wemedia.listen;

import com.heima.common.constants.message.NewsAutoScanConstants;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mrchen
 * @date 2022/3/4 15:16
 */
@Component
@Slf4j
public class WemediaNewsAutoListener {
    @Autowired
    WmNewsAutoScanService wmNewsAutoScanService;
    /**
     * JDK   Integer ==> Integer
     * JSON  Integer ==> String("12314134") ==> String
     * JSON  对象 ==> String("{xxxxxx}") ==> 对象
     *
     * queue: 监听队列   如果队列不存在，不会创建  会报错
     * queueToDeclare  如果队列不存在，会创建队列 不会创建绑定关系
     * bindings 可以声明 队列 + 交换机 + 绑定route key
     * @param newsId
     */
    @RabbitListener(queuesToDeclare = @Queue(NewsAutoScanConstants.WM_NEWS_AUTO_SCAN_QUEUE))
    public void handleAutoScanMsg(String newsId){
        log.info("接收到自动审核的消息   文章id: {}",newsId);
        wmNewsAutoScanService.autoScanWmNews(Integer.valueOf(newsId));
        log.info("审核操作结束   文章id: {}",newsId);
    }
}
