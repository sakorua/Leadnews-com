package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.aliyun.GreenImageScan;
import com.heima.aliyun.GreenTextScan;
import com.heima.common.constants.message.PublishArticleConstants;
import com.heima.common.constants.wemedia.WemediaConstants;
import com.heima.common.exception.CustException;
import com.heima.feigns.AdminFeign;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mrchen
 * @date 2022/3/4 10:37
 */
@Service
@Slf4j
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {

    @Value("${file.oss.web-site}")
    String webSite;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    WmNewsService wmNewsService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void autoScanWmNews(Integer id) {
        // 1. 校验参数  id
        if (id == null){
            log.error(" 文章自动审核失败   原因: 文章id参数错误 {}",id);
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"文章id 不能为空");
        }
        // 2.  根据id查询出文章信息
        WmNews wmNews = wmNewsService.getById(id);
        if (wmNews == null) {
            log.error(" 文章自动审核失败   原因: 该文章不存在  id:{}",id);
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"文章不存在");
        }
        // 3.  检查文章状态   状态: 1
        if (!WemediaConstants.WM_NEWS_SUMMIT_STATUS.equals(wmNews.getStatus())) {
            log.error(" 文章自动审核失败   原因: 该文章的审核状态不为 1   id:{}",id);
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW,"文章不是待审核状态");
        }
        // 4.  抽取出文章的所有  文本内容  和  图片url
        Map<String,Object> contentAndImages = handleTextAndImages(wmNews);
        // 5.  敏感词审核   不通过 状态: 2
        boolean isSensitive = handleSensitive((String)contentAndImages.get("content"),wmNews);
        if (!isSensitive){
            log.info("文章自动审核未通过，内容中包含敏感词");
            return;
        }
        // 6.  阿里云 文本审核   block: 2    review  3
        boolean isTextScan = handleTextScan((String)contentAndImages.get("content"),wmNews);
        if (!isTextScan){
            log.info("文章自动审核未通过，内容中包含违规信息");
            return;
        }
        // 7.  图片审核   block: 2    review  3
        List<String> images = (List<String>)contentAndImages.get("images");
        if (!CollectionUtils.isEmpty(images)) {
            boolean isImageScan = handleImageScan(images,wmNews);
            if (!isImageScan){
                log.info("文章自动审核未通过，图片中包含违规信息");
                return;
            }
        }
        // 8.  修改状态: 8
        updateWmNews(wmNews,WmNews.Status.SUCCESS.getCode(),"审核通过");
        // 9. 发布延时任务，用于发布文章
        // 发布时间
        long publishTime = wmNews.getPublishTime().getTime();
        // 当前时间
        long nowTime = System.currentTimeMillis();
        long remainTime = publishTime - nowTime;
        rabbitTemplate.convertAndSend(PublishArticleConstants.DELAY_DIRECT_EXCHANGE,
                PublishArticleConstants.PUBLISH_ARTICLE_ROUTE_KEY,
                wmNews.getId(),
                message -> {
                    message.getMessageProperties().setHeader("x-delay",remainTime<=0?0:remainTime);
                    return message;
                }
        );
        log.info("成功发送  发布文章消息   待发布的文章id: {}",wmNews.getId());
    }

    @Autowired
    GreenImageScan greenImageScan;

    /**
     * 基于阿里云内容安全 检测图片是否有违规信息
     * @param images
     * @param wmNews
     * @return
     */
    private boolean handleImageScan(List<String> images, WmNews wmNews) {
        boolean flag = false;
        try {
            Map map = greenImageScan.imageUrlScan(images);
            String suggestion = (String) map.get("suggestion");
            switch (suggestion){
                case "block":
                    updateWmNews(wmNews,WmNews.Status.FAIL.getCode(), "图片违规");
                    break;
                case "review":
                    updateWmNews(wmNews,WmNews.Status.ADMIN_AUTH.getCode(), "图片有不确定因素");
                    break;
                case "pass":
                    flag = true;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            updateWmNews(wmNews,WmNews.Status.ADMIN_AUTH.getCode(), "阿里云调用出现异常，转为人工审核");
        }
        return flag;
    }


    @Autowired
    GreenTextScan greenTextScan;
    /**
     * 基于阿里云内容安全  审核文本是否违规
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleTextScan(String content, WmNews wmNews) {
        boolean flag = false;
        try {
            Map map = greenTextScan.greenTextScan(content);
            String suggestion = (String)map.get("suggestion");
            switch (suggestion) {
                case "block":
                    updateWmNews(wmNews,WmNews.Status.FAIL.getCode(), "内容中包含违规信息");
                    break;
                case "review":
                    updateWmNews(wmNews,WmNews.Status.ADMIN_AUTH.getCode(), "内容中包含不确定的信息,需要进一步人工审核");
                    break;
                case "pass":
                    flag = true;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            updateWmNews(wmNews,WmNews.Status.ADMIN_AUTH.getCode(),"阿里云审核调用失败，转为人工审核");
        }
        return flag;
    }

    @Autowired
    AdminFeign adminFeign;
    /**
     * 基于DFA审核敏感词
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleSensitive(String content, WmNews wmNews) {
        boolean flag = true;
        // 1. 远程查询敏感词列表
        ResponseResult<List<String>> sensitivesResult = adminFeign.sensitives();
        if (!sensitivesResult.checkCode()) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        List<String> sensitives = sensitivesResult.getData();
        if (!CollectionUtils.isEmpty(sensitives)) {
            // 2. 基于DFA 审核内容中是否包含敏感词
            SensitiveWordUtil.initMap(sensitives);
            Map<String, Integer> map = SensitiveWordUtil.matchWords(content);
            // 3.  如果包含敏感词  将文章 状态改为 2  设置原因
            if (!CollectionUtils.isEmpty(map)) {
                updateWmNews(wmNews,WmNews.Status.FAIL.getCode(), "内容中包含敏感词  :" + map);
                flag = false;
            }
        }
        return flag;
    }

    /**
     * 修改文章状态
     * @param wmNews
     * @param status
     * @param reason
     */
    private void updateWmNews(WmNews wmNews, short status, String reason) {
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsService.updateById(wmNews);
    }

    /**
     * 抽取文章中所有的内容 和  图片
     * @param wmNews
     * @return   content: string      images: 图片集合
     */
    private Map<String, Object> handleTextAndImages(WmNews wmNews) {
        // 1. 抽取文本内容
        String contentJson = wmNews.getContent();
        if (StringUtils.isBlank(contentJson)) {
            log.error(" 文章自动审核失败   原因: 文章内容为空   id:{}",wmNews.getId());
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"文章内容为空");
        }
        List<Map> contentMap = JSON.parseArray(contentJson, Map.class);
        //    文章内容   content: [{type:text  ,value: },{},{}]
        //   筛选 type=text   获取 value     拼接成字符串
        //  家乡很美_hmtt_国家伟大
        String content = contentMap.stream()
                .filter(m -> "text".equals(m.get("type")))
                .map(m -> (String) m.get("value"))
                .collect(Collectors.joining("_hmtt_"));
        //    文章标题
        content += "_hmtt_" + wmNews.getTitle();
        // 2. 抽取图片内容
        //  筛选内容中的图片 image   获取 value   收集 图片集合
        List<String> images = contentMap.stream()
                .filter(m -> "image".equals(m.get("type")))
                .map(m -> (String) m.get("value"))
                .collect(Collectors.toList());
        // 封面中的图片   wmNews.images (Str url1,url2 )
        String imagesStr = wmNews.getImages();
        // 封面是否为空
        if (StringUtils.isNotBlank(imagesStr)) {
            List<String> imageList = Arrays.stream(imagesStr.split(","))
                    .map(url -> webSite + url)
                    .collect(Collectors.toList());
            images.addAll(imageList);
        }
        // 去重
        images = images.stream().distinct().collect(Collectors.toList());

        // 3. 封装返回结果
        Map<String,Object> map = new HashMap<>();
        map.put("content",content);
        map.put("images",images);
        return map;
    }
}
