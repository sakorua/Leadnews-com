package com.heima.wemedia;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.heima.wemedia.mapper")
public class WmApplication {
    public static void main(String[] args) {
        SpringApplication.run(WmApplication.class, args);
    }
}
