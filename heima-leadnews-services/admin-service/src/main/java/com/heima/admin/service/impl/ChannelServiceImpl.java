package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.ChannelMapper;
import com.heima.admin.service.ChannelService;
import com.heima.model.admin.dtos.ChannelDTO;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * @author SaKoRua
 * @date 2022-02-26 8:01 PM
 * @Description //TODO
 */

@Service
@Slf4j
public class ChannelServiceImpl extends ServiceImpl<ChannelMapper, AdChannel> implements ChannelService {

    @Override
    public ResponseResult findByNameAndPage(ChannelDTO dto) {

        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //  检查分页参数
        dto.checkParam();

        LambdaQueryWrapper<AdChannel> wrapper = Wrappers.lambdaQuery();


        //  模糊查询频道名称
        if (Strings.isNotBlank(dto.getName())) {
            wrapper.like(AdChannel::getName, dto.getName());
        }

        if (dto.getStatus() != null) {
            wrapper.eq(AdChannel::getStatus, dto.getStatus());
        }

        wrapper.orderByAsc(AdChannel::getOrd);

        Page<AdChannel> page = new Page<>(dto.getPage(), dto.getSize());

        IPage<AdChannel> iPage = this.page(page, wrapper);


        return new PageResponseResult(dto.getPage(), dto.getSize(), iPage.getTotal(), iPage.getRecords());
    }

    @Override
    public ResponseResult insert(AdChannel channel) {

        if (Strings.isBlank(channel.getName())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "请输入频道名称");
        }

        if (channel.getName().length() > 10) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "频道名称长度不能大于10");
        }
        int count = this.count(Wrappers.<AdChannel>lambdaQuery().eq(AdChannel::getName, channel.getName()));

        if (count > 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "频道名称已存在");
        }

        channel.setCreatedTime(new Date());
        this.save(channel);
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult update(AdChannel channel) {

        AdChannel channelById = this.getById(channel.getId());

        //  频道不存在
        if (channelById == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //  判断频道名不为空 && 新频道名是否和修改前一致 &&
        if (Strings.isNotBlank(channel.getName()) && !channel.getName().equals(channelById.getName())) {

            if (channel.getName().length() > 10) {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "频道名称长度不能大于10");
            }
            int count = this.count(Wrappers.<AdChannel>lambdaQuery().eq(AdChannel::getName, channel.getName()));

            if (count > 0) {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "频道名称已存在");
            }

        }
        this.updateById(channel);

        return ResponseResult.okResult();
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult deleteById(Integer id) {
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"请输入id");
        }

        AdChannel channelById = this.getById(id);
        if (channelById == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        if (channelById.getStatus()) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW,"活跃中的频道不可删除");
        }


//        System.out.println(1 / 0);


        this.removeById(id);
        return ResponseResult.okResult();
    }


}
