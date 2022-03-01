package com.heima.wemedia.serivce.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustException;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmUserDTO;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.model.wemedia.vos.WmUserVO;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.serivce.WmUserService;
import io.seata.common.util.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;

@Service
public class WmUserServiceImpl extends ServiceImpl<WmUserMapper, WmUser> implements WmUserService {

    @Override
    public ResponseResult login(WmUserDTO dto) {

        //  检查参数
        if (StringUtils.isBlank(dto.getName()) || Strings.isBlank(dto.getPassword())) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }

        WmUser wmUser = this.getOne(Wrappers.<WmUser>lambdaQuery().eq(WmUser::getName, dto.getName()));

        if (wmUser == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        if (wmUser.getStatus().intValue() != 9) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW,"用户状态异常，请联系管理员");
        }

        String md5Str = DigestUtils.md5DigestAsHex(dto.getPassword().getBytes());

        if (!md5Str.equals(wmUser.getPassword())) {
            CustException.cust(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR,"账号或密码错误");
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("token",md5Str);
        WmUserVO wmUserVo = new WmUserVO();
        BeanUtils.copyProperties(wmUser,wmUserVo);
        map.put("user",wmUserVo);

        return ResponseResult.okResult(map);
    }
}