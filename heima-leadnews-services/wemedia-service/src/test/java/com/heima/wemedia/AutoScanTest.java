package com.heima.wemedia;

import com.heima.wemedia.service.WmNewsAutoScanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author mrchen
 * @date 2022/3/4 11:45
 */
@SpringBootTest
public class AutoScanTest {

    @Autowired
    WmNewsAutoScanService wmNewsAutoScanService;

    @Test
    public void autoScan(){
        wmNewsAutoScanService.autoScanWmNews(6270);
    }
}
