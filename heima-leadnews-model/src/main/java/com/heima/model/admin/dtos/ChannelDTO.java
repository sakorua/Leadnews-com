package com.heima.model.admin.dtos;

import com.heima.model.common.dtos.PageRequestDTO;
import lombok.Data;

/**
 * @author SaKoRua
 * @date 2022-02-26 7:57 PM
 * @Description //TODO
 */
@Data
public class ChannelDTO extends PageRequestDTO {
    /**
     * 频道名称
     */
    private String name;
    /**
     * 频道状态
     */
    private Integer status;
}