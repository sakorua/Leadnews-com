package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.AdUserMapper;
import com.heima.admin.service.AdUserService;
import com.heima.common.exception.CustException;
import com.heima.model.admin.dtos.AdUserDTO;
import com.heima.model.admin.pojos.AdUser;
import com.heima.model.admin.vo.AdUserVO;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.common.enums.StatusCode;
import com.heima.utils.common.AppJwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author TaoHongQiang
 * @date Created  2022/2/27 19:26
 * @Description AdUserService 实现类
 */
@Service
public class AdUserServiceImpl extends ServiceImpl<AdUserMapper, AdUser> implements AdUserService {

    /**
     * 登录功能
     *
     * @param dto 前端传入参数 ： 用户名 密码
     * @return 返回实体类
     */
    @Override
    public ResponseResult login(AdUserDTO dto) {
        //参数效验
        if (StringUtils.isBlank(dto.getName()) || StringUtils.isBlank(dto.getPassword())) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //根据用户名获得密码
        AdUser user = getOne(Wrappers.<AdUser>lambdaQuery().eq(AdUser::getName, dto.getName()));
        //判断是否查询到用户名
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "用户名或者密码错误");
        }
        //判断账号状态
        if (user.getStatus() != StatusCode.USER_STATUS_YES.getCode()) {
            CustException.cust(AppHttpCodeEnum.LOGIN_STATUS_ERROR);
        }
        //获得数据库密码 和 盐
        String password = user.getPassword();
        String salt = user.getSalt();
        //加密密码 加盐 存到数据库
        // 用户输入密码（加密后）
        String newPwd = DigestUtils.md5DigestAsHex((dto.getPassword() + salt).getBytes());
        if (!password.equals(newPwd)) {
            CustException.cust(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR, "用户名或密码错误");
        }
        //修改登录时间
        user.setLoginTime(new Date());
        updateById(user);
        //颁发token jwt 令牌
        String token = AppJwtUtil.getToken(Long.valueOf(user.getId()));
        //用户信息返回vo
        AdUserVO vo = new AdUserVO();
        BeanUtils.copyProperties(user, vo);
        //封装结果返回
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("user", user);
        return ResponseResult.okResult(map);
    }

    /**
     * 获取guest密码
     */
    public static void main(String[] args) {
        String salt = "123456";
        String pswd = "guest"+salt;
        String saltPswd = DigestUtils.md5DigestAsHex(pswd.getBytes());
        System.out.println(saltPswd);
    }
}
