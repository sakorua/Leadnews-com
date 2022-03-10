package com.heima.behavior.service.impl;

import com.heima.behavior.service.ApArticleBehaviorService;
import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.common.constants.user.UserRelationConstants;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.ArticleBehaviorDTO;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApCollection;
import com.heima.model.behavior.pojos.ApLikesBehavior;
import com.heima.model.behavior.pojos.ApUnlikesBehavior;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.pojos.ApUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author SaKoRua
 * @date 2022-03-10 4:16 PM
 * @Description //TODO
 */
@Service
public class ApArticleBehaviorServiceImpl implements ApArticleBehaviorService {

    @Autowired
    ApBehaviorEntryService apBehaviorEntryService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public ResponseResult loadArticleBehavior(ArticleBehaviorDTO dto) {

        boolean isfollow = false;
        boolean islike = false;
        boolean isunlike = false;
        boolean iscollection = false;

        //判断用户是否登录
        ApUser user = AppThreadLocalUtils.getUser();

        if (user != null) {
            //如果登录 查询行为实体
            ApBehaviorEntry behaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(), dto.getEquipmentId());
            if (behaviorEntry == null) {
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
            }
            Query query = Query.query(Criteria.where("entryId").is(behaviorEntry.getId()).and("articleId").is(dto.getArticleId()));
            //根据行为实体 文章ID查询 是否点赞
            ApLikesBehavior likesBehavior = mongoTemplate.findOne(query, ApLikesBehavior.class);
            if (likesBehavior != null) {
                islike = true;
            }

            //根据行为实体 文章ID查询 是否不喜欢
            ApUnlikesBehavior unlikesBehavior = mongoTemplate.findOne(query, ApUnlikesBehavior.class);
            if (unlikesBehavior != null) {
                isunlike = true;
            }

            //根据行为实体 文章ID查询 是否收藏
            ApCollection apCollection = mongoTemplate.findOne(query, ApCollection.class);
            if (apCollection != null) {
                iscollection = true;
            }

            //更具登录用户id 去redis中查询是否关注该作者
            Double score = redisTemplate.opsForZSet().score(UserRelationConstants.FOLLOW_LIST + user.getId(), dto.getAuthorApUserId().toString());
            if (score != null) {
                isfollow = true;
            }
        }


        //封装结果 返回
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("isfollow", isfollow);
        resultMap.put("islike", islike);
        resultMap.put("isunlike", isunlike);
        resultMap.put("iscollection", iscollection);
        return ResponseResult.okResult(resultMap);
    }
}
