package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.aliyun.GreenImageScan;
import com.heima.aliyun.GreenTextScan;
import com.heima.common.exception.CustException;
import com.heima.feigns.admin.AdminFeign;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.Content;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsTextAndImages;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author TaoHongQiang
 * @date Created  2022/3/4 17:06
 * @Description
 */
@Service
@Slf4j
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {

    @Autowired
    private WmNewsMapper wmNewsMapper;


    @Value("${file.oss.web-site}")
    String webSite;

    /**
     * 自媒体文章审核
     *
     * @param id 文章id
     */
    @Override
    public void autoScanWmNews(Integer id) {
        //检查参数
        if (id == null) {
            log.error("文章审核出错，id不存在 文章ID：{}", id);
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "文章id不存在");
        }

        //调用接口查询文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (wmNews == null) {
            log.error("文章查询不到，文章ID：{}", "id");
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "文章不存在");
        }


        //判断状态 看看是否需要审核
        Short wmNewsStatus = wmNews.getStatus();
        if (WmNews.Status.SUBMIT.getCode() == wmNewsStatus) {
            //需要审核 获得文章的文本 和图片集合
            WmNewsTextAndImages wmNewsTextAndImages = getAllContentAndImages(wmNews);


            //敏感词审核 失败状态改为2
            boolean isSensitive = handleSensitive(wmNewsTextAndImages.getContent(), wmNews);
            if (!isSensitive) {
                return;
            }
            log.info("自管理敏感词审核通过");


            //阿里云审核 状态 失败修改为2 不确定 3
            boolean isTextScan = handleTextScan(wmNewsTextAndImages.getContent(), wmNews);
            if (!isTextScan) {
                return;
            }
            log.info("阿里云内容审核通过");

            //图片审核 失败  状态2  不确定 状态3
            List<String> images = wmNewsTextAndImages.getImages();
            if (images != null) {
                boolean isImageScan = handleImageScan(images, wmNews);
                if (!isImageScan) return;
                log.info(" 阿里云图片审核通过  =======   ");
            }

            //判断文章发布时间是否大于当前时间   状态 8
            updateWmNews(wmNews, WmNews.Status.SUCCESS.getCode(), "审核成功");


            // TODO 通知定时发布任务
        }


    }

    @Autowired
    private GreenImageScan greenImageScan;

    /**
     * 阿里云图片审核
     *
     * @param images 图片
     * @param wmNews pojo类
     * @return 通过返回ture 失败返回false
     */
    private boolean handleImageScan(List<String> images, WmNews wmNews) {
        boolean flag = true;
        try {
            Map map = greenImageScan.imageUrlScan(images);
            String suggestion = (String) map.get("suggestion");
            switch (suggestion) {
                case "block":
                    updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "图片中有违规内容，审核失败");
                    flag = false;
                    break;
                case "review":
                    updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "图片中有不确定内容，转为人工审核");
                    flag = false;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("阿里云图片审核出现异常 , 原因:{}", e.getMessage());
            updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "阿里云内容服务异常，转为人工审核");
            flag = false;
        }
        return flag;
    }


    @Autowired
    private GreenTextScan greenTextScan;

    /**
     * 阿里云审核 失败返回false 成功返回ture block: 状态2    review: 状态3    异常: 状态3
     *
     * @param content 文本
     * @param wmNews  pojo
     * @return 返回结果
     */
    private boolean handleTextScan(String content, WmNews wmNews) {
        boolean flag = true;
        try {
            Map map = greenTextScan.greenTextScan(content);
            String suggestion = (String) map.get("suggestion");
            switch (suggestion) {
                case "block":
                    updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "文本中有违规内容，审核失败");
                    flag = false;
                    break;
                case "review":
                    updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "文本中有不确定内容，转为人工审核");
                    flag = false;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("阿里云文本审核出现异常 , 原因:{}", e.getMessage());
            updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "阿里云内容服务异常，转为人工审核");
            flag = false;
        }
        return flag;
    }

    @Autowired
    private AdminFeign adminFeign;

    /**
     * 判断是否存在敏感词 存在返回false 不存在返回true
     *
     * @param content 文本
     * @param wmNews  pojo
     * @return 结果
     */
    private boolean handleSensitive(String content, WmNews wmNews) {
        boolean flag = true;
        //查询出数据库的敏感词
        ResponseResult<List<String>> result = adminFeign.findAllSensitives();
        if (result.getCode() != 0) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR, result.getErrorMessage());
        }
        List<String> allSensitives = result.getData();
        //调用工具类
        SensitiveWordUtil.initMap(allSensitives);
        //检测敏感词
        Map<String, Integer> resultMap = SensitiveWordUtil.matchWords(content);
        if (resultMap.size() > 0) {
            //将文章状态修改为2
            updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "内容中包含敏感词" + resultMap);
            flag = false;
        }
        return flag;
    }

    /**
     * 修改文章状态
     *
     * @param wmNews pojo
     * @param status 状态
     * @param reason 信息
     */
    private void updateWmNews(WmNews wmNews, short status, String reason) {
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    /**
     * 抽取文本内容所有方法
     *
     * @param wmNews 文章pojo
     * @return 返回pojo
     */
    private WmNewsTextAndImages getAllContentAndImages(WmNews wmNews) {
        //获取数据库文章内容
        String newContent = wmNews.getContent();
        //封装为对象
        List<Content> contentList = JSON.parseArray(newContent, Content.class);
        //抽取文本
        String text = contentList.stream()
                .filter((content1) -> "text".equals(content1.getType()))
                .map(Content::getValue)
                .collect(Collectors.joining("__"));
        //添加标题
        text = text + "__" + wmNews.getTitle();
        //抽取图片url
        List<String> urlImages = contentList.stream()
                .filter((content) -> "image".equals(content.getType()))
                .map(Content::getValue)
                .collect(Collectors.toList());
        //添加封面
        String images = wmNews.getImages();
        if (StringUtils.isNotBlank(images)) {
            //切割数据 以,切割
            List<String> collect = Arrays.stream(images.split(","))
                    .map((url) -> webSite + url)
                    .collect(Collectors.toList());
            //添加到集合里
            urlImages.addAll(collect);
        }

        //去除 url集合里的重复元素
        urlImages = urlImages.stream()
                .distinct()
                .collect(Collectors.toList());


        return new WmNewsTextAndImages(text, urlImages);
    }
}
