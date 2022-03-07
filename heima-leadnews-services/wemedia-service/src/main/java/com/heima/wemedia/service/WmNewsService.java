package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDTO;
import com.heima.model.wemedia.dtos.WmNewsDTO;
import com.heima.model.wemedia.dtos.WmNewsPageReqDTO;
import com.heima.model.wemedia.pojos.WmNews;

public interface WmNewsService extends IService<WmNews> {
    /**
     * 分页查找文章
     * @param dto 前端dto参数
     * @return 返回结果
     */
    ResponseResult findList(WmNewsPageReqDTO dto);

    /**
     * 自媒体文章发布
     *
     * @param dto 前端传入参数
     * @return 返回结果
     */
    ResponseResult submitNews(WmNewsDTO dto);


    /**
     * 根据id查询文章
     * @param id id
     * @return 返回值
     */
    ResponseResult findById(Integer id);

    /**
     * 根据id删除文章
     * @param id id
     * @return 返回状态
     */
    ResponseResult deleteById(Integer id);

    /**
     * 文章上下架操作
     * @param dto {
     *              enable:0
     *              id : 1
     *            }
     * @return 返回状态
     */
    ResponseResult downOrUpNews(WmNewsDTO dto);

    /**
     * 查询文章列表
     * @param dto 前端请求
     * @return  返回
     */
    ResponseResult findAllNewsList(NewsAuthDTO dto);

    /**
     * 查询文章详情
     * @param id 文章id
     * @return 返回vo对象
     */
    ResponseResult findWmNewsVo(Integer id);

    /**
     *  自媒体文章人工审核
     * @param status  2  审核失败  4 审核成功
     * @param dto
     * @return
     */
    ResponseResult updateStatus(Short status, NewsAuthDTO dto);
}