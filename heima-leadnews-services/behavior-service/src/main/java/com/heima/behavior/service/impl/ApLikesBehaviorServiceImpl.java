package com.heima.behavior.service.impl;

import com.heima.behavior.filter.AppTokenFilter;
import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApLikesBehaviorService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.LikesBehaviorDTO;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApLikesBehavior;
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

import static com.heima.model.common.enums.AppHttpCodeEnum.NEED_LOGIN;
import static com.heima.model.common.enums.AppHttpCodeEnum.PARAM_INVALID;

/**
 * @author SaKoRua
 * @date 2022-03-08 2:44 PM
 * @Description //TODO
 */
@Service
public class ApLikesBehaviorServiceImpl implements ApLikesBehaviorService {

    @Autowired
    ApBehaviorEntryService apBehaviorEntryService;

    @Autowired
    MongoTemplate mongoTemplate;

    /**
     * 点赞或取消点赞
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult like(LikesBehaviorDTO dto) {

        // 1. 校验参数

        //点赞需要登录
        if (AppThreadLocalUtils.getUser() == null) {
            CustException.cust(NEED_LOGIN);
        }
        ApUser user = AppThreadLocalUtils.getUser();
        //校验文章id不能为空     使用注解校验

        //校验点赞方式 只能是0 或 1   使用注解校验


        // 2. 根据当前登录用户id查询行为实体对象
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(), dto.getEquipmentId());
        if (apBehaviorEntry == null) {
            CustException.cust(NEED_LOGIN);
        }


        Query query = Query.query(Criteria.where("entryId").is(apBehaviorEntry.getRefId()).and("articleId").is(dto.getArticleId()));
        ApLikesBehavior apLikesBehavior = mongoTemplate.findOne(query, ApLikesBehavior.class);
        // 3. 如果是点赞操作  判断是否已经点过赞
        if (dto.getOperation().intValue() == 0 && apLikesBehavior != null) {
            CustException.cust(AppHttpCodeEnum.DATA_EXIST);
        }
        if (dto.getOperation().intValue() == 0) {
            // 4. 没有点过赞则 像mongo点赞集合中 添加点赞数据
            ApLikesBehavior behavior = new ApLikesBehavior();
            behavior.setEntryId(apBehaviorEntry.getId());
            behavior.setArticleId(dto.getArticleId());
            behavior.setType((short) 0);
            behavior.setOperation((short) 0);
            behavior.setCreatedTime(new Date());
            mongoTemplate.save(behavior);
        }
        // 5. 如果是取消点赞操作  在mongo点赞集合中 删除对应点赞数据
        if (dto.getOperation().intValue() == 1){
            mongoTemplate.remove(query,ApLikesBehavior.class);
        }
        return ResponseResult.okResult();
    }
}
