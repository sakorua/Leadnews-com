package com.heima.wemedia;

import com.heima.aliyun.GreenImageScan;
import com.heima.aliyun.GreenTextScan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Map;

/**
 * @author mrchen
 * @date 2022/3/4 9:38
 */
@SpringBootTest
public class AliyunSecurityTest {
    @Autowired
    GreenTextScan greenTextScan;

    @Autowired
    GreenImageScan greenImageScan;

    @Test
    public void textScan(){
        try {
            Map map = greenTextScan.greenTextScan("贩卖冰毒是违法的");
            String suggestion = (String)map.get("suggestion");
            System.out.println(suggestion);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("阿里云 内容审核调用 失败");
        }
    }

    @Test
    public void imageScan(){
        try {
            Map map = greenImageScan.imageUrlScan(Arrays.asList(
                    "https://hmtt139.oss-cn-shanghai.aliyuncs.com/material/2022/3/20220302/mv004.jpg"
            ));
            String suggestion = (String)map.get("suggestion");
            System.out.println(suggestion);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("阿里云 内容审核调用 失败");
        }
    }
}
