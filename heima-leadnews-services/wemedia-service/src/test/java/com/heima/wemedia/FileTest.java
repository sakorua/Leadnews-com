package com.heima.wemedia;

import com.heima.file.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * @author TaoHongQiang
 * @date Created  2022/3/1 18:25
 * @Description
 */
@SpringBootTest
public class FileTest {

    @Value("${file.oss.web-site}")
    String webSite;

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    public void Test01() throws FileNotFoundException {
//        FileInputStream inputStream = new FileInputStream(new File("/Users/taohongqiang/Documents/DesktopWallpaper/WechatIMG191.jpeg"));
//        String upload = fileStorageService.store("upload", "aaa1.jpg", inputStream);
//        System.out.println(webSite+upload);

        fileStorageService.delete("upload/2022/3/20220301/aaa1.jpg");
    }
}
