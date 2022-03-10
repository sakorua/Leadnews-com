package com.heima.behavior.service.impl;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApCollectionBehaviorService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.CollectionBehaviorDTO;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApCollection;
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
 * @date 2022-03-10 3:55 PM
 * @Description //TODO
 */
@Service
public class ApCollectionBehaviorServiceImpl implements ApCollectionBehaviorService {

    @Autowired
    ApBehaviorEntryService apBehaviorEntryService;
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public ResponseResult collectBehavior(CollectionBehaviorDTO dto) {
        ApUser user = AppThreadLocalUtils.getUser();
        //校验参数 需要登录 文章id不能null  操作类型 0 或 1
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }

        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(), dto.getEquipmentId());
        if (apBehaviorEntry == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //根据userId查询行为实体数据
        Query query = Query.query(Criteria.where("entryId").is(apBehaviorEntry.getId()).and("articleId").is(dto.getArticleId()));
        ApCollection apCollection = mongoTemplate.findOne(query, ApCollection.class);

        //如果是收藏操作 判断是否已经收藏
        if (apCollection != null && dto.getOperation().intValue() == 0) {
            CustException.cust(AppHttpCodeEnum.DATA_EXIST);
        }

        //如果未收藏 新增收藏行为
        if (dto.getOperation().intValue() == 0) {
            apCollection = new ApCollection();
            apCollection.setEntryId(apBehaviorEntry.getId());
            apCollection.setArticleId(dto.getArticleId());
            apCollection.setType((short) 0);
            apCollection.setCollectionTime(new Date());
            mongoTemplate.save(apCollection);
        }
        //如果是取消收藏操作  删除收藏行为
        if (dto.getOperation().intValue() == 1) {
            mongoTemplate.remove(query, ApCollection.class);
        }
        return ResponseResult.okResult();
    }
}
