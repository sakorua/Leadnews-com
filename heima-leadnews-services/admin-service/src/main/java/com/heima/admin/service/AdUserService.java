package com.heima.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.admin.dtos.AdUserDTO;
import com.heima.model.admin.pojos.AdUser;
import com.heima.model.common.dtos.ResponseResult;

/**
 * @author TaoHongQiang
 * @date Created  2022/2/27 19:26
 * @Description userService 接口
 */
public interface AdUserService extends IService<AdUser> {
    /**
     * 登录功能
     *
     * @param dto 前端传入参数 ： 用户名 密码
     * @return 返回实体类
     */
    ResponseResult login(AdUserDTO dto);
}
