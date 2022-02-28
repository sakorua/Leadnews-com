package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.AdSensitiveMapper;
import com.heima.admin.service.AdSensitiveService;
import com.heima.model.admin.dtos.SensitiveDTO;
import com.heima.model.admin.pojos.AdSensitive;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * @author SaKoRua
 * @date 2022-02-27 5:20 PM
 * @Description //TODO
 */
@Service
@Transactional
public class AdSensitiveServiceImpl extends ServiceImpl<AdSensitiveMapper, AdSensitive> implements AdSensitiveService {
    @Override
    public ResponseResult list(SensitiveDTO dto) {

        //  检查分页参数
        dto.checkParam();

        //  设置分页参数
        Page page = new Page(dto.getPage(), dto.getSize());

        LambdaQueryWrapper<AdSensitive> wrapper = Wrappers.lambdaQuery();

        // 设置模糊查询
        if (StringUtils.isNotBlank(dto.getName())) {
            wrapper.like(AdSensitive::getSensitives, dto.getName());
        }

        IPage pageResult = this.page(page, wrapper);

        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), pageResult.getTotal());
        responseResult.setData(pageResult.getRecords());


        return responseResult;
    }

    @Override
    public ResponseResult insert(AdSensitive adSensitive) {

        //  检查请求参数
        if (adSensitive == null || StringUtils.isBlank(adSensitive.getSensitives())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "请输入需要添加的敏感词");
        }

        //  检查是否已经有此敏感词
        LambdaQueryWrapper<AdSensitive> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(AdSensitive::getSensitives, adSensitive.getSensitives());

        if (this.count(wrapper) > 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST);
        }


        adSensitive.setCreatedTime(new Date());
        this.save(adSensitive);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult update(AdSensitive adSensitive) {

        if(adSensitive.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        LambdaQueryWrapper<AdSensitive> wrapper = Wrappers.lambdaQuery();
        int count = this.count(wrapper.eq(AdSensitive::getSensitives, adSensitive.getSensitives()));

        if (count > 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST);
        }

        this.updateById(adSensitive);


        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Override
    public ResponseResult delete(Integer id) {

        if (id ==null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        AdSensitive sensitive = this.getById(id);
        if (sensitive == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        this.removeById(id);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
