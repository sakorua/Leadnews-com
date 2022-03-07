package com.heima.wemedia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.wemedia.dtos.NewsAuthDTO;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.vos.WmNewsVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WmNewsMapper extends BaseMapper<WmNews> {

    /**
     * 分页查询
     * @param dto 参数
     * @return 返回值
     */
    List<WmNewsVO> findListAndPage(@Param("dto") NewsAuthDTO dto);

    /**
     * 统计多少数据
     * @param dto 参数
     * @return 返回值
     */
    long findListCount(@Param("dto") NewsAuthDTO dto);
}