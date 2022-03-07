package com.heima.wemedia;

import com.heima.wemedia.service.WmNewsAutoScanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author TaoHongQiang
 * @date Created  2022/3/4 19:07
 * @Description 测试审核功能
 */
@SpringBootTest
public class WmNewsAutoScanServiceTest {

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Test
    public void autoScanServiceTest(){
        wmNewsAutoScanService.autoScanWmNews(6270);
    }
}
