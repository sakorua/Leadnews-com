package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.admin.AdminConstants;
import com.heima.common.exception.CustException;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.model.wemedia.pojos.WmUserDTO;
import com.heima.model.wemedia.vos.WmUserVO;
import com.heima.utils.common.AppJwtUtil;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mrchen
 * @date 2022/2/27 14:18
 */
@Service
public class WmUserServiceImpl extends ServiceImpl<WmUserMapper, WmUser> implements WmUserService {
    @Override
    public ResponseResult login(WmUserDTO dto) {
        // 1. 校验参数
        String name = dto.getName();
        String password = dto.getPassword();
        if (StringUtils.isBlank(name)||StringUtils.isBlank(password)){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"用户名或密码不能为空");
        }
        // 2. 根据用户名查询用户
        WmUser user = this.getOne(Wrappers.<WmUser>lambdaQuery().eq(WmUser::getName, name));
        if (user==null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"用户不存在");
        }
        // 3. 对比密码
        String inputPwd = DigestUtils.md5DigestAsHex((password + user.getSalt()).getBytes());
        if(!inputPwd.equals(user.getPassword())){
            CustException.cust(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR,"密码错误");
        }
        // 4. 查看用户状态
        if (!AdminConstants.USER_STATUS_ALLOW.equals(user.getStatus())) {
            CustException.cust(AppHttpCodeEnum.LOGIN_STATUS_ERROR);
        }
        // 5. 修改最近登陆时间
        user.setLoginTime(new Date());
        this.updateById(user);
        // 6. 颁发token
        String token = AppJwtUtil.getToken(Long.valueOf(user.getId()));
        // 7.  封装返回结果  user     token
        Map map = new HashMap<>();
        map.put("token",token);
        WmUserVO wmUserVO = new WmUserVO();
        BeanUtils.copyProperties(user,wmUserVO);
        map.put("user",wmUserVO);
        return ResponseResult.okResult(map);
    }
}
