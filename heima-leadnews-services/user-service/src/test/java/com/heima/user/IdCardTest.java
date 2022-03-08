package com.heima.user;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mrchen
 * @date 2022/3/1 10:41
 */

public class IdCardTest {

    @Test
    public void idCardDemo(){
        RestTemplate restTemplate = new RestTemplate();
        // 封装请求头
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("apicode","0a33c9a1562c46c39c1604d10ea3f493");
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        // 请求参数
        Map<String,String> map = new HashMap<>();
        map.put("idNumber","210103195103222114");
        map.put("userName","王东镇");
        HttpEntity<String> httpEntity = new HttpEntity<>(JSON.toJSONString(map), httpHeaders);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity("https://api.yonyoucloud.com/apis/dst/matchIdentity/matchIdentity", httpEntity, String.class);
        System.out.println(responseEntity.getBody());
    }

}
