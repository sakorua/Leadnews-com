package com.heima.model.behavior.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CollectionBehaviorDTO {
    // 设备ID
    Integer equipmentId;
    // 文章、动态ID
    @NotNull
    @JsonAlias("entryId") // 前端变量命名entryId 实际为articleId 因此起个别名
            Long articleId;
    /**
     * 收藏内容类型
     * 0文章
     * 1动态
     */
    Short type;
    /**
     * 操作类型
     * 0收藏
     * 1取消收藏
     */
    @Size(min = 0, max = 1)
    Short operation;
}