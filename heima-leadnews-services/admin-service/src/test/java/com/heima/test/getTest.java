package com.heima.test;

import org.junit.jupiter.api.Test;
import org.springframework.util.DigestUtils;

/**
 * @author SaKoRua
 * @date 2022-02-27 8:21 PM
 * @Description //TODO
 */
public class getTest {

    @Test
    public void test(){
        String salt = "124806211";
        String pswd = "admin"+salt;
        String saltPswd = DigestUtils.md5DigestAsHex(pswd.getBytes());
        System.out.println(saltPswd);
    }
}
