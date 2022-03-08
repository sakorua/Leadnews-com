package com.heima.article;

import com.heima.article.service.ApArticleService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author mrchen
 * @date 2022/3/5 10:54
 */
@SpringBootTest
public class PublishArticleTest {
    @Autowired
    ApArticleService apArticleService;

    @Test
    public void publish(){
        apArticleService.publishArticle(6269);
    }
}
