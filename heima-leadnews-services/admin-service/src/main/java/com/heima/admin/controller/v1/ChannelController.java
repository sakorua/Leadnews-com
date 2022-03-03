package com.heima.admin.controller.v1;

import com.heima.admin.service.ChannelService;
import com.heima.model.admin.dtos.ChannelDTO;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author SaKoRua
 * @date 2022-02-26 8:20 PM
 * @Description //TODO
 */
@RestController
@RequestMapping("api/v1/channel")
public class ChannelController {

    @Autowired
    private ChannelService channelService;


    @PostMapping("list")
    ResponseResult list(@RequestBody ChannelDTO dto) {
        return channelService.findByNameAndPage(dto);
    }

    @PostMapping("save")
    ResponseResult save(@RequestBody AdChannel channel) {
        return channelService.insert(channel);
    }

    @PostMapping("update")
    ResponseResult update(@RequestBody AdChannel channel) {
        return channelService.update(channel);
    }

    @GetMapping("del/{id}")
    ResponseResult delete(@PathVariable("id") Integer id) {
        return channelService.deleteById(id);
    }

    @ApiOperation("查询全部频道")
    @GetMapping("/channels")
    public ResponseResult findAll() {
        List<AdChannel> list = channelService.list();
        return ResponseResult.okResult(list);
    }
}
