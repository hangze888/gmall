package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;


@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(){
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //cors配置对象
        corsConfiguration.addAllowedOrigin("http://localhost:1000");//允许的域
        corsConfiguration.addAllowedHeader("*");//允许的头信息
        corsConfiguration.addAllowedMethod("*");//允许的请求方式
        corsConfiguration.setAllowCredentials(true);//是否允许携带cookie信息

        UrlBasedCorsConfigurationSource corsConfigurationSource = new UrlBasedCorsConfigurationSource();
        corsConfigurationSource.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(corsConfigurationSource);
    }
}
