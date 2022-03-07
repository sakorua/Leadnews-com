package com.heima.admin;

import com.heima.admin.mapper.AdUserMapper;
import com.heima.admin.service.AdUserService;
import com.heima.model.admin.dtos.AdUserDTO;
import com.heima.model.admin.pojos.AdUser;
import com.heima.model.common.dtos.ResponseResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author TaoHongQiang
 * @date Created  2022/3/1 20:08
 * @Description
 */
@SpringBootTest
public class DemoTest {
    @Autowired
    AdUserMapper adUserMapper;

    @Autowired
    AdUserService service;

    @Test
    public void test01(){
//        AdUserDTO adUserDTO = new AdUserDTO();
//        adUserDTO.setName("guest");
//        adUserDTO.setPassword("guest");
//        ResponseResult login = service.login(adUserDTO);
//        System.out.println("login = " + login);

        AdUser adUser = adUserMapper.selectById(1);
        System.out.println("adUser = " + adUser);
    }
}
