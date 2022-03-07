package com.heima.model.wemedia.pojos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author TaoHongQiang
 * @date Created  2022/3/4 17:14
 * @Description 文章内容 和图片集合
 */
@Data
@AllArgsConstructor
public class WmNewsTextAndImages {
    /**
     * 文本内容合集
     */
    private String content;
    /**
     * 图片集合
     */
    private List<String> images;
}
