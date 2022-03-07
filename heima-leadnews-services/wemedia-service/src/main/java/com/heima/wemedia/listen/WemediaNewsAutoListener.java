package com.heima.wemedia.listen;

import com.heima.common.constants.wemedia.NewsAutoScanConstants;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author TaoHongQiang
 * @date Created  2022/3/4 19:51
 * @Description
 */
@Component
@Slf4j
public class WemediaNewsAutoListener {

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @RabbitListener(queuesToDeclare = @Queue(NewsAutoScanConstants.WM_NEWS_AUTO_SCAN_QUEUE))
    public void newsAutoScanHandler(String newsId) {
        log.info("接收到 自动审核消息 {}", newsId);
        //自动审核
        wmNewsAutoScanService.autoScanWmNews(Integer.valueOf(newsId));
    }
}
