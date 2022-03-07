package com.heima.feigns.admin;

import com.heima.feigns.config.HeimaFeignAutoConfiguration;
import com.heima.feigns.fallback.AdminFeignFallback;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * @author TaoHongQiang
 * @date Created  2022/3/4 16:58
 * @Description admin feign调用
 */
@FeignClient(value = "leadnews-admin",
        fallbackFactory = AdminFeignFallback.class,
        configuration = HeimaFeignAutoConfiguration.class)
public interface AdminFeign {

    /**
     * 查询所有敏感词 放入list集合
     *
     * @return 返回list集合
     */
    @PostMapping("/api/v1/sensitive/sensitives")
    ResponseResult<List<String>> findAllSensitives();

    @GetMapping("/api/v1/channel/one/{id}")
    public ResponseResult<AdChannel> findOne(@PathVariable Integer id);
}
