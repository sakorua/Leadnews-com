package com.heima.user.service.impl;

import com.heima.common.constants.user.UserRelationConstants;
import com.heima.common.exception.CustException;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.threadlocal.WmThreadLocalUtils;
import com.heima.model.user.dtos.UserRelationDTO;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.service.ApUserRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author SaKoRua
 * @date 2022-03-07 9:19 PM
 * @Description //TODO
 */
@Service
public class ApUserRelationServiceImpl implements ApUserRelationService {

    @Autowired
    StringRedisTemplate redisTemplate;


    @Override
    public ResponseResult follow(UserRelationDTO dto) {

        //  检查参数
        if (dto.getAuthorApUserId() == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }

        if (dto.getOperation() == null || (dto.getOperation() != 0 && dto.getOperation() != 1)) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 用户ID
        Integer userId = user.getId();
        //  作者ID
        Integer authorApUserId = dto.getAuthorApUserId();

        if (userId.equals(authorApUserId)) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW);
        }

        //检查是否已关注

        Double score = redisTemplate.opsForZSet().score(UserRelationConstants.FOLLOW_LIST + userId, String.valueOf(authorApUserId));
        if (dto.getOperation().intValue() == 0 && score != null) {
            CustException.cust(AppHttpCodeEnum.DATA_EXIST, "您已关注，请勿重复关注");
        }

        if (dto.getOperation().intValue() == 0) {
            //用户添加关注
            redisTemplate.opsForZSet().add(UserRelationConstants.FOLLOW_LIST + userId, String.valueOf(authorApUserId), System.currentTimeMillis());

            //作者添加粉丝
            redisTemplate.opsForZSet().add(UserRelationConstants.FANS_LIST + authorApUserId,String.valueOf(userId),System.currentTimeMillis());
        }else{
            //用户取消关注
            redisTemplate.opsForZSet().remove(UserRelationConstants.FOLLOW_LIST + userId,String.valueOf(authorApUserId));

            //作者删除粉丝
            redisTemplate.opsForZSet().remove(UserRelationConstants.FANS_LIST + authorApUserId,String.valueOf(userId));
        }
        return ResponseResult.okResult();
    }
}
