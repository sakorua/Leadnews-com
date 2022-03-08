package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.AdUserMapper;
import com.heima.admin.service.AdUserService;
import com.heima.common.exception.CustException;
import com.heima.model.admin.dtos.AdUserDTO;
import com.heima.model.admin.pojos.AdUser;
import com.heima.model.admin.vos.AdUserVO;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.utils.common.AppJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mrchen
 * @date 2022/2/27 9:52
 */
@Service
@Slf4j
public class AdUserServiceImpl extends ServiceImpl<AdUserMapper, AdUser> implements AdUserService {

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult login(AdUserDTO dto) {
        // 1. 校验参数   用户名 和 密码
        String name = dto.getName();
        String password = dto.getPassword();
        if(StringUtils.isBlank(name) || StringUtils.isBlank(password)){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"用户名或密码不能为空");
        }
        // 2. 根据用户名查询用户信息
        AdUser user = getOne(Wrappers.<AdUser>lambdaQuery().eq(AdUser::getName, name));
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"用户信息不存在");
        }
        // 3. 判断密码是否正确
        String inputPwd = DigestUtils.md5DigestAsHex((password + user.getSalt()).getBytes());
        if(!inputPwd.equals(user.getPassword())){
            CustException.cust(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR,"输入密码不正确");
        }
        // 4. 检查用户状态是否为  9
        if (user.getStatus().intValue() != 9) {
            CustException.cust(AppHttpCodeEnum.LOGIN_STATUS_ERROR,"用户状态异常，请联系管理员");
        }
        // 5. 更新登陆的时间
        user.setLoginTime(new Date());
        updateById(user);
        // 6. 颁发token
        String token = AppJwtUtil.getToken(Long.valueOf(user.getId()));
        // 7.  封装响应结果   data: {  token :    user : }
        Map<String,Object> result = new HashMap<>();
        result.put("token",token);
        // 封装vo返回
        AdUserVO adUserVO = new AdUserVO();
        BeanUtils.copyProperties(user,adUserVO);
        result.put("user",adUserVO);
        return ResponseResult.okResult(result);
    }
}
