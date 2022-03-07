package com.itheima;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * @author TaoHongQiang
 * @date Created  2022/3/1 17:17
 * @Description
 */
public class demo01 {
    public static void main(String[] args) throws FileNotFoundException {
        // Endpoint以杭州为例，其它Region请按实际情况填写。
        String endpoint = "oss-cn-shanghai.aliyuncs.com";
        // 阿里云主账号AccessKey拥有所有API的访问权限，风险很高。
        String accessKeyId = "LTAI4FxcdHpzZf4DJZVhcn37";
        String accessKeySecret = "wlevncOt3W5kVYFhWX1Akio82P2Kgt";

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 上传Byte数组。
        FileInputStream inputStream = new FileInputStream("/Users/taohongqiang/Documents/DesktopWallpaper/WechatIMG190.jpeg");
        PutObjectResult result = ossClient.putObject("gangan0623", "material/a.jpg", inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();

    }
}
