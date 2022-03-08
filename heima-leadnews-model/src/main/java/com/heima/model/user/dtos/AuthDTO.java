package com.heima.model.user.dtos;

import com.heima.model.common.dtos.PageRequestDTO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AuthDTO extends PageRequestDTO {
    //状态
    @ApiModelProperty("审核状态")
    private Short status;
    // 认证用户ID
    @ApiModelProperty("认证用户ID")
    private Integer id;
    //驳回的信息
    @ApiModelProperty("驳回的信息")
    private String msg;
}