package com.heima.article.config;

import com.heima.common.constants.message.PublishArticleConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author mrchen
 * @date 2022/3/5 11:05
 */
@Configuration
public class DeclarePubArticleRabbitConfig {
    @Bean
    public DirectExchange directExchange(){
        return ExchangeBuilder.directExchange(PublishArticleConstants.DELAY_DIRECT_EXCHANGE)
                .delayed()
                .build();
    }
    @Bean
    public Queue queue(){
        return QueueBuilder.durable(PublishArticleConstants.PUBLISH_ARTICLE_QUEUE).build();
    }
    @Bean
    public Binding binding(){
        return BindingBuilder.bind(queue()).to(directExchange()).with(PublishArticleConstants.PUBLISH_ARTICLE_ROUTE_KEY);
    }
}
