package com.heima.model.admin.dtos;

import com.heima.model.common.dtos.PageRequestDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author mrchen
 * @date 2022/2/26 10:30
 */
@Data
@ApiModel("频道查询请求参数DTO")
public class ChannelDTO extends PageRequestDTO {
    @ApiModelProperty("频道名称")
    private String name;
    @ApiModelProperty("频道状态")
    private Integer status;
}
