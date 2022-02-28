package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.AdUserMapper;
import com.heima.admin.service.AdUserService;
import com.heima.common.exception.CustomException;
import com.heima.model.admin.dtos.AdUserDTO;
import com.heima.model.admin.pojos.AdUser;
import com.heima.model.admin.vos.AdUserVO;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.utils.common.AppJwtUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;

@Service
public class AdUserServiceImpl extends ServiceImpl<AdUserMapper, AdUser> implements AdUserService {

    @Override
    public ResponseResult login(AdUserDTO dto) {


        if (StringUtils.isBlank(dto.getName()) || StringUtils.isBlank(dto.getPassword())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        LambdaQueryWrapper<AdUser> wrapper = Wrappers.<AdUser>lambdaQuery();
        wrapper.eq(AdUser::getName, dto.getName());

        AdUser dbUser = this.getOne(wrapper);

        if (dbUser == null) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST, "账号或密码错误");
        }

        if (9 != dbUser.getStatus().intValue()) {
            throw new CustomException(AppHttpCodeEnum.LOGIN_STATUS_ERROR);
        }


        //  获取密码和盐值
        String dbPwd = dbUser.getPassword();
        String salt = dbUser.getSalt();

        String inputPwd = DigestUtils.md5DigestAsHex((dto.getPassword() + salt).getBytes());

        if (!inputPwd.equals(dbPwd)) {
            throw new CustomException(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR, "账号或密码错误");
        }


        dbUser.setLoginTime(new Date());
        this.updateById(dbUser);

        String token = AppJwtUtil.getToken(dbUser.getId().longValue());

        AdUserVO adUserVO = new AdUserVO();
        BeanUtils.copyProperties(dbUser, adUserVO);

        HashMap<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("user", adUserVO);


        return ResponseResult.okResult(map);
    }
}