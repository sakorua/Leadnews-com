package com.heima.wemedia;

import com.heima.file.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * @author mrchen
 * @date 2022/3/1 14:30
 */
@SpringBootTest
public class UploadTest {
    @Autowired
    FileStorageService fileStorageService;

    @Value("${file.oss.prefix}")
    String prefix;

    @Value("${file.oss.web-site}")
    String webSite;
    @Test
    public void upload() throws FileNotFoundException {
        FileInputStream in = new FileInputStream("C:\\worksoft\\picture\\0004.jpg");

        // 返回值:  文件在oss中的路径    prefix + 年月日文件目录 + 文件名称
        String store = fileStorageService.store(prefix, "body01.jpg", in);

        // 返回值:  文件在oss中的路径   webSite + prefix + 年月日文件目录 + 文件名称
        System.out.println(webSite + store);
    }

    @Test
    public void delete() throws FileNotFoundException {
        // 返回值:  文件在oss中的路径    prefix + 年月日文件目录 + 文件名称
        fileStorageService.delete("material/2022/3/20220301/body01.jpg");
    }
}
