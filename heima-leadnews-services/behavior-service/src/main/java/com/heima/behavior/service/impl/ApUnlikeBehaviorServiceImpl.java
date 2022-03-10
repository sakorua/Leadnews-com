package com.heima.behavior.service.impl;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApUnlikeBehaviorService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.UnLikesBehaviorDTO;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApUnlikesBehavior;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.pojos.ApUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author SaKoRua
 * @date 2022-03-10 3:40 PM
 * @Description //TODO
 */
@Service
public class ApUnlikeBehaviorServiceImpl implements ApUnlikeBehaviorService {

    @Autowired
    ApBehaviorEntryService apBehaviorEntryService;
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public ResponseResult unlikeBehavior(UnLikesBehaviorDTO dto) {
        //校验参数 文章id不能为空  需要登录 不喜欢类型取值为 0 或 1
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        //查询行为实体数据
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(), dto.getEquipmentId());
        if (apBehaviorEntry == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }

        Query query = Query.query(Criteria.where("entryId").is(apBehaviorEntry.getId()).and("articleId").is(dto.getArticleId()));
        ApUnlikesBehavior unlikesBehavior = mongoTemplate.findOne(query, ApUnlikesBehavior.class);

        //如果是 不喜欢操作 查询不喜欢行为是否存在
        if (unlikesBehavior != null && dto.getType().intValue() == 0) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //如果不存在   添加不喜欢行为
        if (dto.getType().intValue() == 0) {
            ApUnlikesBehavior behavior = new ApUnlikesBehavior();
            behavior.setArticleId(dto.getArticleId());
            behavior.setEntryId(apBehaviorEntry.getId());
            behavior.setCreatedTime(new Date());
            behavior.setType((short) 0);
        }
        //如果是 取消不喜欢操作  删除对应的不喜欢数据
        if (dto.getType().intValue() == 1) {
            mongoTemplate.remove(query, ApUnlikesBehavior.class);
        }
        return ResponseResult.okResult();
    }
}
