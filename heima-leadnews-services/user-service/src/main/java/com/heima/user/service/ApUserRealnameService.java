package com.heima.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.AuthDTO;
import com.heima.model.user.pojos.ApUserRealname;

/**
 * @author TaoHongQiang
 * @date Created  2022/2/27 21:16
 * @Description
 */
public interface ApUserRealnameService extends IService<ApUserRealname> {

    ResponseResult loadListByStatus(AuthDTO dto);

    /**
     * 根据状态进行审核
     * @param dto
     * @param status  2 审核失败   9 审核成功
     * @return
     */
    ResponseResult updateStatusById(AuthDTO dto, Short status);
}
