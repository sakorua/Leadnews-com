package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.ChannelMapper;
import com.heima.admin.service.ChannelService;
import com.heima.common.exception.CustException;
import com.heima.common.exception.CustomException;
import com.heima.model.admin.dtos.ChannelDTO;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;

/**
 * @author mrchen
 * @date 2022/2/26 10:32
 */
@Service
@Slf4j
public class ChannelServiceImpl extends ServiceImpl<ChannelMapper, AdChannel> implements ChannelService {
    @Override
    public ResponseResult findByNameAndPage(ChannelDTO dto) {
        // 1. 校验参数
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        // 检查分页参数
        dto.checkParam();
        // 2. 条件查询
        LambdaQueryWrapper<AdChannel> wrapper = Wrappers.lambdaQuery();
        //   构建条件对象 wrapper
        if(StringUtils.isNotBlank(dto.getName())){
            wrapper.like(AdChannel::getName,dto.getName()) ;
        }
        if(dto.getStatus()!=null){
            wrapper.eq(AdChannel::getStatus,dto.getStatus());
        }
        wrapper.orderByAsc(AdChannel::getOrd);
        //   构建分页对象  page
        Page<AdChannel> pageReq = new Page<>(dto.getPage(), dto.getSize());
        //   执行查询封装返回结果
        IPage<AdChannel> pageResult = this.page(pageReq, wrapper);
        return new PageResponseResult(dto.getPage(),dto.getSize(),pageResult.getTotal(),pageResult.getRecords());
    }

    @Override
    public ResponseResult insert(AdChannel channel) {
        // 1. 参数校验   名称    长度    不可以重复
        String name = channel.getName();
        if(StringUtils.isBlank(name)){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"频道名称不能为空");
        }
        if (name.length() > 10) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"频道名称长度不能大于10");
        }
        int count = this.count(Wrappers.<AdChannel>lambdaQuery().eq(AdChannel::getName, name));
        if (count>0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"该频道名称已经存在");
        }
        // 2. 新增频道
        channel.setCreatedTime(new Date());
        this.save(channel);
        // 3. 返回结果
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult update(AdChannel adChannel) {
        String name = adChannel.getName();
        // 1. 参数校验
        AdChannel channel = this.getById(adChannel.getId());
        if(channel == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        // 如果name为空  不用校验    java     java
        // 如果name不为空  判断name 和 修改前是否一致  如果不一致  判断是否重复   长度
        if (StringUtils.isNotBlank(name)&&!name.equals(channel.getName())) {
            if (name.length() > 10) {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"频道名称长度不能大于10");
            }
            int count = this.count(Wrappers.<AdChannel>lambdaQuery().eq(AdChannel::getName, name));
            if (count>0) {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"该频道名称已经存在");
            }
        }
        // 2. 修改频道
        this.updateById(adChannel);
        // 3. 响应结果
        return ResponseResult.okResult();
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult deleteById(Integer id) {
//        if(id == 110){
//            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"id不能等于110");
//        }
        // 1. 校验参数   id   如果频道有效 不能删除
        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"id不能为空");
        }
        AdChannel channel = this.getById(id);
        if(channel == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        // 写的操作
        if (channel.getStatus()) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW,"频道有效，不能删除");
        }
        // 2. 删除频道
        this.removeById(id);
        return ResponseResult.okResult();
    }

    public static void main(String[] args) {
        //随机生成一个10位数的随机字符串
        String salt = RandomStringUtils.randomAlphabetic(10);
        System.out.println("salt = " + salt);

        String pwd = "123" + salt;
        System.out.println("pwd = " + pwd);

        String s = DigestUtils.md5DigestAsHex(pwd.getBytes());
        System.out.println("s = " + s);
    }


}
