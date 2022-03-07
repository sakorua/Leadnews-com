package com.heima.wemedia.service;

/**
 * @author TaoHongQiang
 * @date Created  2022/3/4 17:05
 * @Description
 */
public interface WmNewsAutoScanService {
    /**
     * 自媒体文章审核
     * @param id 文章id
     */
    void autoScanWmNews(Integer id);
}
