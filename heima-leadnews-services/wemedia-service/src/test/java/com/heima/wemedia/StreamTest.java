package com.heima.wemedia;


import com.alibaba.fastjson.JSON;
import com.heima.model.wemedia.pojos.Content;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author TaoHongQiang
 * @date Created  2022/3/4 17:28
 * @Description
 */
public class StreamTest {
    @Test
    public void demo01(){
        String json = "[\n" +
                "{\"type\":\"image\",\"value\":\"http://gangan0623.oss-cn-shanghai.aliyuncs.com/material/2022/3/20220302/f38dba93b96c4a1486484d8c7f7c5909.jpeg\"},\n" +
                "{\"type\":\"text\",\"value\":\"333333333333331\"},\n" +
                "{\"type\":\"text\",\"value\":\"我焯\"},\n" +
                "{\"type\":\"image\",\"value\":\"http://gangan0623.oss-cn-shanghai.aliyuncs.com/material/2022/3/20220301/7bfc737c3c1f4866a681df95b3e4381a.jpeg\"}\n" +
                "]";

        //封装为对象
        List<Content> contentList =  JSON.parseArray(json,Content.class);
        //抽取文本
        String collect = contentList.stream()
                .filter((content1) -> "text".equals(content1.getType()))
                .map(Content::getValue)
                .collect(Collectors.joining("__"));
        System.out.println("collect = " + collect);

//        //抽取图片url
//        List<Content> image = contentList.stream()
//                .filter((content) -> content.getType().equals("image"))
//                .collect(Collectors.toList());

        List<String> collect1 = contentList.stream()
                .filter((content)-> "image".equals(content.getType()))
                .map(Content::getValue)
                .collect(Collectors.toList());
        System.out.println("collect1 = " + collect1);

        System.out.println(collect1.get(0));

    }
}
