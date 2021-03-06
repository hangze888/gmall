package com.atguigu.gmall.auth.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.utils.CookieUtils;
import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.service.AuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtProperties jwtProperties;

    @PostMapping("accredit")
    public Resp<Object> accredit(@RequestParam("username") String username, @RequestParam("password") String password, HttpServletRequest request, HttpServletResponse response){
        String token = authService.accredit(username, password);
        if(StringUtils.isNotBlank(token)){
            //4.把jwt放入cookie
            CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),token,jwtProperties.getExpireTime()*60);
        }
        return Resp.ok(null);
    }
}
