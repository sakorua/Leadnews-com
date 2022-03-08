package com.heima.admin.controller.v1;

import com.heima.admin.service.AdUserService;
import com.heima.model.admin.dtos.AdUserDTO;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mrchen
 * @date 2022/2/27 10:03
 */
@Api(value = "admin登陆api",tags = "admin登陆api")
@RestController
public class LoginController {
    @Autowired
    AdUserService adUserService;
    @ApiOperation(value = "admin端登陆方法",notes = "")
    @PostMapping("login/in")
    public ResponseResult login(@RequestBody AdUserDTO dto){
        return adUserService.login(dto);
    }
}
