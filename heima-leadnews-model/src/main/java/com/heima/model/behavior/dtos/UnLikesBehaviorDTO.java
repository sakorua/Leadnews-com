package com.heima.model.behavior.dtos;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class UnLikesBehaviorDTO {
    // 设备ID
    Integer equipmentId;
    // 文章ID
    @NotNull
    Long articleId;
    /**
     * 不喜欢操作方式
     * 0 不喜欢
     * 1 取消不喜欢
     */
    @Size(min =0 , max = 1)
    Short type;

}