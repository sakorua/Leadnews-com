package com.heima.wemedia.filter;

import com.alibaba.fastjson.JSON;
import com.heima.wemedia.utils.AppJwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mrchen
 * @date 2022/2/27 10:18
 */
@Component
@Order(-1)
@Slf4j
public class AuthorizeFilter implements GlobalFilter {
    private static List<String> urlList = new ArrayList<>();
    // 初始化白名单 url路径
    static {
        urlList.add("/login/in");
        urlList.add("/v2/api-docs");
    }
    /**
     * @param exchange  包含  请求对象  响应对象
     * @param chain  过滤器链
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 获取请求对象 请求路径
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();
        // 2. 判断请求路径是否属于白名单路径
        for (String allowUrl : urlList) {
            if (path.contains(allowUrl)) {
                // 2.1  如果是白名单路径 直接放行
                return chain.filter(exchange);
            }
        }
        // 3. 如果不是，获取请求头中的token信息
        String token = request.getHeaders().getFirst("token");
        // 3.1  如果无token 直接终止请求  返回401
        if(StringUtils.isBlank(token)){
            return writeMessage(response,"未携带token");
        }
        // 4. 如果有token 解析token
        try {
            Claims claimsBody = AppJwtUtil.getClaimsBody(token);
            int i = AppJwtUtil.verifyToken(claimsBody);
            if (i < 1){
                // 4.1 解析成功  获取token中的userId  并设置到请求header转发到其它服务
                Object id = claimsBody.get("id");
                request.mutate().header("userId",String.valueOf(id));
                log.info ("token解析成功 ,  当前登陆用户id :  {}",id);
                return chain.filter(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("token解析失败 , {}",e.getMessage());
        }
        // 4.2 解析失败  终止请求  返回401
        return writeMessage(response,"token解析失败");
    }

    private Mono<Void> writeMessage(ServerHttpResponse response, String message) {
        // 1. 响应结果内容
        Map map = new HashMap<>();
        map.put("code", HttpStatus.UNAUTHORIZED.value());
        map.put("message", message);
        // 2. 设置响应头 状态  响应内容
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 将内容写给前端
        DataBuffer dataBuffer = response.bufferFactory().wrap(JSON.toJSONBytes(map));
        return response.writeWith(Flux.just(dataBuffer));
    }
}
