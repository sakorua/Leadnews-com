package com.heima.user;

import com.heima.feigns.ArticleFeign;
import com.heima.feigns.WemediaFeign;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author mrchen
 * @date 2022/2/27 14:37
 */
@SpringBootTest
public class FeignTest {
    @Autowired
    WemediaFeign wemediaFeign;

    @Autowired
    ArticleFeign articleFeign;

    @Test
    public void wemediaFeignTest(){
        ResponseResult<WmUser> admin = wemediaFeign.findByName("admin");
        System.out.println(admin);
    }

    @Test
    public void articleFeignTest(){
        ResponseResult<ApAuthor> byUserId = articleFeign.findByUserId(4);
        System.out.println(byUserId);
    }
}
