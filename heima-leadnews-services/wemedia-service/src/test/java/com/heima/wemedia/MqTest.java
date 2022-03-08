package com.heima.wemedia;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author mrchen
 * @date 2022/3/4 14:54
 */
@SpringBootTest
public class MqTest {
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Test
    public void sendMsg(){
        rabbitTemplate.convertAndSend("","xxxx","hello msg");
    }
}
