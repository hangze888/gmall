package com.atguigu.gmall.gateway.filter;


import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@EnableConfigurationProperties({JwtProperties.class})
public class AuthGatewayFilter implements GatewayFilter {

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        //获取jwt类型的token信息
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        if(CollectionUtils.isEmpty(cookies) || !cookies.containsKey(jwtProperties.getCookieName())){
            //如果cookie为空，或者cookie中不包含token信息
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            //后续业务不执行
            return response.setComplete();
        }
        //获取token
        HttpCookie cookie = cookies.getFirst(jwtProperties.getCookieName());
        if(cookie == null){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            //后续业务不执行
            return response.setComplete();
        }
        String token = cookie.getValue();

        //如果token为空
        if(StringUtils.isEmpty(token)){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            //后续业务不执行
            return response.setComplete();
        }


        //解析token信息
        try {
            //正常解析放行
            JwtUtils.getInfoFromToken(token,jwtProperties.getPublicKey());
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            //后续业务不执行
            return response.setComplete();
        }

        return chain.filter(exchange);
    }
}
