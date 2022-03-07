package com.heima.model.admin.dtos;

import com.heima.model.common.dtos.PageRequestDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author TaoHongQiang
 * @date Created  2022/2/26 20:29
 * @Description
 */
@Data
@ApiModel("敏感词分页查询请求参数")
public class SensitiveDTO extends PageRequestDTO {
    @ApiModelProperty("敏感词名称")
    private String name;
}
