package com.heima.behavior.service.impl;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApReadBehaviorService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.ReadBehaviorDTO;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApReadBehavior;
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
 * @date 2022-03-10 3:14 PM
 * @Description //TODO
 */
@Service
public class ApReadBehaviorServiceImpl implements ApReadBehaviorService {

    @Autowired
    ApBehaviorEntryService apBehaviorEntryService;
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public ResponseResult readBehavior(ReadBehaviorDTO dto) {

        ApUser user = AppThreadLocalUtils.getUser();

        //校验参数 文章id必须传
        if (dto.getArticleId() == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        //根据登录用户id 或 设备id查询行为实体数据(阅读操作可不登录)
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user == null ? null : user.getId(), dto.getEquipmentId());
        if (apBehaviorEntry == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        //判断阅读行为是否存在
        Query query = Query.query(Criteria.where("entryId").is(apBehaviorEntry.getId()).and("articleId").is(dto.getArticleId()));
        ApReadBehavior apReadBehavior = mongoTemplate.findOne(query, ApReadBehavior.class);

        if (apReadBehavior == null) {
            //不存在  创建阅读行为 并初始化count字段值为 1
            ApReadBehavior behavior = new ApReadBehavior();
            behavior.setArticleId(dto.getArticleId());
            behavior.setEntryId(apBehaviorEntry.getId());
            behavior.setCreatedTime(new Date());
            behavior.setUpdatedTime(new Date());
            behavior.setCount((short) 1);
            mongoTemplate.save(behavior);
        } else {
            //存在  将阅读行为的count字段加1 并修改
            apReadBehavior.setCount((short) (apReadBehavior.getCount() + 1));
            mongoTemplate.save(apReadBehavior);
        }

        return ResponseResult.okResult();
    }
}
