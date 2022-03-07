package com.heima.feigns.fallback;

import com.heima.feigns.admin.AdminFeign;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author TaoHongQiang
 * @date Created  2022/3/4 16:59
 * @Description
 */
@Component
@Slf4j
public class AdminFeignFallback implements FallbackFactory<AdminFeign> {

    @Override
    public AdminFeign create(Throwable cause) {
        return new AdminFeign() {
            @Override
            public ResponseResult<List<String>> findAllSensitives() {
                log.error("ArticleFeign findByUserId 远程调用出错啦 ~~~ !!!! {} ",cause.getMessage());
                return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR);
            }

            @Override
            public ResponseResult findOne(Integer id) {
                log.info("参数: {}",id);
                log.error("AdminFeign findOne 远程调用出错啦 ~~~ !!!! {} ",cause.getMessage());
                return ResponseResult.errorResult(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
            }
        };
    }
}
