package com.heima.feigns;

import com.heima.feigns.fallback.AdminFeignFallback;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * @author mrchen
 * @date 2022/3/4 10:33
 */
@FeignClient(value = "leadnews-admin",fallbackFactory = AdminFeignFallback.class)
public interface AdminFeign {
    @PostMapping("/api/v1/sensitive/sensitives")
    public ResponseResult<List<String>> sensitives();

    // ================新增接口方法  start ================
    @GetMapping("/api/v1/channel/one/{id}")
    public ResponseResult<AdChannel> findOne(@PathVariable Integer id);
    // ================新增接口方法  end ================
}
