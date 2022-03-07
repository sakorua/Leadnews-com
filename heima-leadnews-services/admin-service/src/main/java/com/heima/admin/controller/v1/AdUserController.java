package com.heima.admin.controller.v1;

import com.heima.admin.service.AdUserService;
import com.heima.model.admin.dtos.AdUserDTO;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author TaoHongQiang
 * @date Created  2022/2/27 19:25
 * @Description user控制层
 */
@RestController
@RequestMapping("/login")
@Api(value = "用户登录API",tags = "用户登录API")
public class AdUserController {

    @Autowired
    private AdUserService userService;


    /**
     * 登录功能
     * @param dto 前端传入参数 ： 用户名 密码
     * @return 返回实体类
     */
    @ApiOperation("登录功能")
    @PostMapping("/in")
    public ResponseResult login(@RequestBody AdUserDTO dto){
        return userService.login(dto);
    }


}
