package com.heima.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.admin.dtos.ChannelDTO;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.ResponseResult;

public interface ChannelService extends IService<AdChannel> {

    ResponseResult findByNameAndPage(ChannelDTO dto);

    ResponseResult insert(AdChannel channel);

    ResponseResult update(AdChannel channel);

    ResponseResult deleteById(Integer id);
}
