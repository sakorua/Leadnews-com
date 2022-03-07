package com.heima.admin.controller.v1;

import com.heima.admin.service.SensitiveService;
import com.heima.model.admin.dtos.SensitiveDTO;
import com.heima.model.admin.pojos.AdSensitive;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author TaoHongQiang
 * @date Created  2022/2/26 20:06
 * @Description
 */
@RestController
@Api(value = "敏感词管理API",tags = "敏感词管理API")
@RequestMapping("/api/v1/sensitive")
public class SensitiveController {

    @Autowired
    private SensitiveService sensitiveService;

    /**
     * 根据id删除敏感词
     * @param id id
     * @return 返回响应状态
     */
    @ApiOperation(value = "根据id删除敏感词")
    @DeleteMapping("/del/{id}")
    public ResponseResult deleteSensitiveById(@PathVariable Integer id){
        return sensitiveService.deleteSensitiveById(id);
    }

    /**
     * 根据用户名分页查询敏感词列表
     * @param dto 前端请求参数 名字 分页大小 页码
     * @return 返回分页数据集合
     */
    @ApiOperation("根据用户名分页查询敏感词列表")
    @PostMapping("/list")
    public ResponseResult SelectSensitiveByName(@RequestBody SensitiveDTO dto){
        return sensitiveService.SelectSensitiveByName(dto);
    }

    /**
     * 新建敏感词信息
     * @param adSensitive 敏感词pojo数据
     * @return 返回状态
     */
    @ApiOperation("新建敏感词信息")
    @PostMapping("/save")
    public ResponseResult insertSensitive(@RequestBody AdSensitive adSensitive){
        return sensitiveService.insertSensitive(adSensitive);
    }

    /**
     * 修改敏感词信息
     * @param adSensitive 敏感词pojo类
     * @return 返回响应状态
     */
    @ApiOperation("修改敏感词信息")
    @PostMapping("/update")
    public ResponseResult updateSensitive(@RequestBody AdSensitive adSensitive){
        return sensitiveService.updateSensitive(adSensitive);
    }

    /**
     * 查询所有敏感词 放入list集合
     * @return 返回list集合
     */
    @ApiOperation("查询所有敏感词")
    @PostMapping("/sensitives")
    public ResponseResult<List<String>> findAllSensitives(){
        return sensitiveService.findAllSensitives();
    }
}
