package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.exception.CustException;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.LoginDTO;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserLoginService;
import com.heima.utils.common.AppJwtUtil;
import io.seata.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;

import static com.heima.model.common.enums.AppHttpCodeEnum.*;

/**
 * @author SaKoRua
 * @date 2022-03-07 5:45 PM
 * @Description //TODO
 */
@Service
@Slf4j
public class ApUserLoginServiceImpl implements ApUserLoginService {

    @Autowired
    ApUserMapper apUserMapper;

    @Override
    public ResponseResult login(LoginDTO dto) {

        if (StringUtils.isNotBlank(dto.getPhone()) && Strings.isNotBlank(dto.getPassword())) {

            //  根据前端填入的手机号查询对应的用户
            ApUser apUser = apUserMapper.selectOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, dto.getPhone()));
            if (apUser == null) {
                CustException.cust(DATA_NOT_EXIST);
            }
            //  判断账号密码是否正确
            String inputPwdMd5 = DigestUtils.md5DigestAsHex((dto.getPassword() + apUser.getSalt()).getBytes());

            if (!inputPwdMd5.equals(apUser.getPassword())) {
                CustException.cust(LOGIN_PASSWORD_ERROR);
            }

            //  判断账户状态是否可以登录
            if (apUser.getStatus()) {
                CustException.cust(LOGIN_STATUS_ERROR);
            }

            String token = AppJwtUtil.getToken(Long.valueOf(apUser.getId()));

            Map map = new HashMap();

            map.put("token", token);
            map.put("user", apUser);

            return ResponseResult.okResult(map);
        } else {

            if (dto.getEquipmentId() == null) {
                CustException.cust(PARAM_INVALID);
            }
            String token = AppJwtUtil.getToken(0L);

            Map map = new HashMap();

            map.put("token", token);
            return ResponseResult.okResult(map);
        }

    }
}
