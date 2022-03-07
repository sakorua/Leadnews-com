package com.heima.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.admin.dtos.SensitiveDTO;
import com.heima.model.admin.pojos.AdSensitive;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author TaoHongQiang
 * @date Created  2022/2/26 20:08
 * @Description
 */
public interface SensitiveService extends IService<AdSensitive> {
    /**
     * 根据id删除敏感词
     *
     * @param id id
     * @return 返回响应状态
     */
    ResponseResult deleteSensitiveById(Integer id);

    /**
     * 根据用户名分页查询敏感词列表
     * @param dto 前端请求参数 名字 分页大小 页码
     * @return 返回分页数据集合
     */
    ResponseResult SelectSensitiveByName(SensitiveDTO dto);

    /**
     * 新建敏感词信息
     * @param adSensitive 敏感词pojo数据
     * @return 返回状态
     */
    ResponseResult insertSensitive(AdSensitive adSensitive);

    /**
     * 修改敏感词信息
     * @param adSensitive 敏感词pojo类
     * @return 返回响应状态
     */
    ResponseResult updateSensitive(AdSensitive adSensitive);

    /**
     * 查询所有敏感词 放入list集合
     * @return 返回list集合
     */
    ResponseResult<List<String>> findAllSensitives();
}
