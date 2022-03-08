package com.heima.article.service.impl;
import com.alibaba.fastjson.JSONArray;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.mapper.AuthorMapper;
import com.heima.article.service.GeneratePageService;
import com.heima.common.exception.CustException;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.enums.AppHttpCodeEnum;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static com.heima.model.common.enums.AppHttpCodeEnum.SERVER_ERROR;

@Service
@Slf4j
public class GeneratePageServiceImpl implements GeneratePageService {
    @Autowired
    private Configuration configuration;
    @Resource(name = "minIOFileStorageService")
    private FileStorageService fileStorageService;
    @Value("${file.minio.prefix}")
    private String prefix;
    @Autowired
    ApArticleMapper apArticleMapper;
    @Autowired
    AuthorMapper authorMapper;
    /**
     * 生成文章静态页面
     */
    @Override
    public void generateArticlePage(String content, ApArticle apArticle) {
        try {
            Template template = configuration.getTemplate("article.ftl");
            HashMap<String, Object> params = new HashMap<>();
            params.put("content", JSONArray.parseArray(content));
            params.put("article", apArticle);
            ApAuthor author = authorMapper.selectById(apArticle.getAuthorId());
            params.put("authorApUserId", author.getUserId());

            StringWriter writer = new StringWriter();
            template.process(params,writer);

            ByteArrayInputStream in = new ByteArrayInputStream(writer.toString().getBytes());
            String store = fileStorageService.store(prefix, apArticle.getId() + ".html", "text/html", in);
            apArticle.setStaticUrl(store);
        } catch (Exception e) {
            e.printStackTrace();
            CustException.cust(SERVER_ERROR);
        }
    }
}