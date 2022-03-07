package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.SensitiveMapper;
import com.heima.admin.service.SensitiveService;
import com.heima.common.exception.CustException;
import com.heima.model.admin.dtos.SensitiveDTO;
import com.heima.model.admin.pojos.AdSensitive;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author TaoHongQiang
 * @date Created  2022/2/26 20:08
 * @Description 敏感词管理
 */
@Service
@Slf4j
public class SensitiveServiceImpl extends ServiceImpl<SensitiveMapper, AdSensitive> implements SensitiveService {

    /**
     * 根据id删除敏感词
     *
     * @param id id
     * @return 返回响应状态
     */
    @Override
    public ResponseResult deleteSensitiveById(Integer id) {
        //判断参数
        if (id <= 0) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //执行方法
        boolean b = this.removeById(id);
        return ResponseResult.okResult();
    }

    /**
     * 根据用户名分页查询敏感词列表
     *
     * @param dto 前端请求参数 名字 分页大小 页码
     * @return 返回分页数据集合
     */
    @Override
    public ResponseResult SelectSensitiveByName(SensitiveDTO dto) {
        // 1. 校验参数
        if (dto == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //检查参数
        dto.checkParam();
        String name = dto.getName();
        Page<AdSensitive> adSensitivePage = new Page<>(dto.getPage(), dto.getSize());
        //过滤条件
        LambdaQueryWrapper<AdSensitive> wrapper = Wrappers.lambdaQuery();
        //如果不为空执行
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(AdSensitive::getSensitives, name);
        }
        //执行分页
        IPage<AdSensitive> page = page(adSensitivePage, wrapper);
        //封装参数
        return new PageResponseResult(dto.getPage(), dto.getSize(), page.getTotal(), page.getRecords());
    }

    /**
     * 新建敏感词信息
     *
     * @param adSensitive 敏感词pojo数据
     * @return 返回状态
     */
    @Override
    public ResponseResult insertSensitive(AdSensitive adSensitive) {
        //参数判断
        String name = adSensitive.getSensitives();
        //判断名字是否为空
        if (StringUtils.isBlank(name)) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //设置时间
        adSensitive.setCreatedTime(new Date());
        //插入条件
        boolean save = this.save(adSensitive);
        return ResponseResult.okResult();
    }

    /**
     * 修改敏感词信息
     *
     * @param adSensitive 敏感词pojo类
     * @return 返回响应状态
     */
    @Override
    public ResponseResult updateSensitive(AdSensitive adSensitive) {
        //判断是否有id
        if (adSensitive.getId() <= 0) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //判断是否有敏感词
        if (StringUtils.isBlank(adSensitive.getSensitives())) {
            CustException.cust(AppHttpCodeEnum.PARAM_REQUIRE, "请填写要修改的敏感词类型");
        }
        //设置更新时间
        adSensitive.setCreatedTime(new Date());
        //插入条件
        LambdaUpdateWrapper<AdSensitive> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(AdSensitive::getId, adSensitive.getId());
        //更新
        boolean update = this.update(adSensitive, wrapper);
        if (!update) {
            log.error("更新失败");
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "服务器异常，请稍后重试");
        }
        return ResponseResult.okResult();

    }

    @Autowired
    private SensitiveMapper sensitiveMapper;

    /**
     * 查询所有敏感词 放入list集合
     * @return 返回list集合
     */
    @Override
    public ResponseResult<List<String>> findAllSensitives() {
        return ResponseResult.okResult(sensitiveMapper.findAllSensitives());
    }
}
