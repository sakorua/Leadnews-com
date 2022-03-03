package com.heima.wemedia.serivce;
import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.media.dtos.WmNewsDTO;
import com.heima.model.wemedia.dtos.WmNewsPageReqDTO;
import com.heima.model.wemedia.pojos.WmNews;
public interface WmNewsService extends IService<WmNews> {
    /**
     * 查询所有自媒体文章
     * @return
     */
    public ResponseResult findList(WmNewsPageReqDTO dto);

    /**
     * 自媒体文章发布
     * @return
     */
    ResponseResult submitNews(WmNewsDTO dto);

    ResponseResult findWmNewsById(Integer id);


    ResponseResult delNews(Integer id);

    ResponseResult downOrUp(WmNewsDTO dto);
}