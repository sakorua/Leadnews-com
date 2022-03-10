package com.heima.behavior.service.impl;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author SaKoRua
 * @date 2022-03-08 3:07 PM
 * @Description //TODO
 */
@Service
public class ApBehaviorEntryServiceImpl implements ApBehaviorEntryService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public ApBehaviorEntry findByUserIdOrEquipmentId(Integer userId, Integer equipmentId) {

        //1. 判断userId是否为空
        if (userId != null) {
            //不为空 使用userId查询 行为实体 (refId = 用户id     type = 用户类型  )
            Query query = Query.query(Criteria.where("refId").is(userId).and("type").is(ApBehaviorEntry.Type.USER.getCode()));
            ApBehaviorEntry apBehaviorEntry = mongoTemplate.findOne(query, ApBehaviorEntry.class);
            //如果不存在基于userId创建实体数据
            if (apBehaviorEntry == null) {
                apBehaviorEntry = new ApBehaviorEntry();
                apBehaviorEntry.setType(ApBehaviorEntry.Type.USER.getCode());
                apBehaviorEntry.setRefId(userId);
                apBehaviorEntry.setCreatedTime(new Date());
                mongoTemplate.save(apBehaviorEntry);
            }
            return apBehaviorEntry;
            //返回行为实体
        } else
            //2. 判断设备id是否为空
            if (equipmentId != null) {
                //不为空 使用设备id查询  行为实体 (refId = 设备id   type = 设备类型)
                Query query = Query.query(Criteria.where("refId").is(equipmentId).and("type").is(ApBehaviorEntry.Type.EQUIPMENT.getCode()));
                ApBehaviorEntry apBehaviorEntry = mongoTemplate.findOne(query, ApBehaviorEntry.class);
                //如果不存在基于设备id创建实体数据
                if (apBehaviorEntry == null) {
                    apBehaviorEntry = new ApBehaviorEntry();
                    apBehaviorEntry.setType(ApBehaviorEntry.Type.EQUIPMENT.getCode());
                    apBehaviorEntry.setRefId(equipmentId);
                    apBehaviorEntry.setCreatedTime(new Date());
                }
                return apBehaviorEntry;
                //返回行为实体
            }

        //3. 如果userId和设备id都不存在  返回null 调用者需要判断null
        return null;
    }
}
