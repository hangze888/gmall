package com.atguigu.gmall.cart.interceptor;

import com.atguigu.core.utils.CookieUtils;
import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.core.bean.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;


//目的获取userId以及userKey
@EnableConfigurationProperties({JwtProperties.class})
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserInfo userInfo = new UserInfo();

        String token = CookieUtils.getCookieValue(request, jwtProperties.getCookieName());
        String userKey = CookieUtils.getCookieValue(request, jwtProperties.getUserKey());
        //判断userKey是否为空
        if(StringUtils.isEmpty(userKey)){
            //如果为空制作一个放入cookie中
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request,response,jwtProperties.getUserKey(),userKey,jwtProperties.getExpireTime());
        }

        userInfo.setUserKey(userKey);

        if(StringUtils.isEmpty(token)){
            //把userInfo传递给后续的业务
            THREAD_LOCAL.set(userInfo);
            return true;
        }

        try {
            Map<String, Object> infoFromToken = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
            Long id = Long.valueOf(infoFromToken.get("id").toString());
            userInfo.setUserId(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //把userInfo传递给后续的业务
        THREAD_LOCAL.set(userInfo);

        return true;
    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //防止内存泄漏，线程池：请求结束不代表线程结束
        THREAD_LOCAL.remove();
    }
}
