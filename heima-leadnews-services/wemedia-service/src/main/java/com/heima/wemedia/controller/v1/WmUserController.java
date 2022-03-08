package com.heima.wemedia.controller.v1;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.service.WmUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author mrchen
 * @date 2022/2/27 14:19
 */
@RestController
@RequestMapping("/api/v1/user")
public class WmUserController {
    @Autowired
    private WmUserService wmUserService;

    @GetMapping("/findByName/{name}")
    public ResponseResult findByName(@PathVariable("name") String name){
        WmUser user = wmUserService.getOne(Wrappers.<WmUser>lambdaQuery().eq(WmUser::getName, name));
        return ResponseResult.okResult(user);
    }
    @PostMapping("/save")
    public ResponseResult save(@RequestBody WmUser wmUser){
        wmUserService.save(wmUser);
        return ResponseResult.okResult(wmUser);
    }
}
