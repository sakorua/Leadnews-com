package com.heima.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.admin.pojos.AdSensitive;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author TaoHongQiang
 * @date Created  2022/2/26 20:07
 * @Description
 */
public interface SensitiveMapper extends BaseMapper<AdSensitive> {
    /**
     * 查询所有敏感词 放入list集合
     * @return 返回list集合
     */
    @Select("select sensitives from leadnews_admin.ad_sensitive")
    List<String> findAllSensitives();
}
