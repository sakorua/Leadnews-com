package com.heima.admin.controller.v1;

import com.heima.admin.service.ChannelService;
import com.heima.model.admin.dtos.ChannelDTO;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author mrchen
 * @date 2022/2/26 10:45
 */
@RestController
@RequestMapping("api/v1/channel")
@Api(value = "频道管理API",tags = "频道管理API")
public class ChannelController {
    @Autowired
    ChannelService channelService;

    @ApiOperation(value = "分页条件查询",notes = "按照 频道名称、频道状态 进行分页查询，结果以序号升序排序")
    @PostMapping("list")
    public ResponseResult list(@RequestBody ChannelDTO dto){
        return channelService.findByNameAndPage(dto);
    }

    @ApiOperation("查询全部频道")
    @GetMapping("/channels")
    public ResponseResult findAll() {
        List<AdChannel> list = channelService.list();
        return ResponseResult.okResult(list);
    }

    @ApiOperation(value = "新增频道")
    @PostMapping("save")
    public ResponseResult save(@RequestBody AdChannel channel){
        return channelService.insert(channel);
    }

    @ApiOperation(value = "修改频道")
    @PostMapping("update")
    public ResponseResult update(@RequestBody AdChannel channel){
        return channelService.update(channel);
    }

    @ApiOperation(value = "删除频道")
    @GetMapping("del/{id}")
    public ResponseResult delete(@PathVariable("id") Integer id){
        return channelService.deleteById(id);
    }

    @ApiOperation("根据id查询频道")
    @GetMapping("/one/{id}")
    public ResponseResult findOne(@PathVariable Integer id) {
        return ResponseResult.okResult(channelService.getById(id));
    }
}
