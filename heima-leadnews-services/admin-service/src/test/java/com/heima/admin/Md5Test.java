package com.heima.admin;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.util.DigestUtils;

/**
 * @author mrchen
 * @date 2022/2/27 9:10
 */
public class Md5Test {


    @Test
    public void md5Test(){
        // 盐  用于混淆密码的字符串
        String salt = RandomStringUtils.randomAlphanumeric(10);
        System.out.println(salt);
        // 输入密码
        String  input = "hello";

        String pwd = DigestUtils.md5DigestAsHex((input + salt).getBytes());

        System.out.println(pwd);
    }
}
