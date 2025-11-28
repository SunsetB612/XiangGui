package com.xianggui.app.config;

import com.xianggui.app.interceptor.JwtTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtTokenInterceptor jwtTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtTokenInterceptor)
                // 排除不需要token的接口
                .excludePathPatterns(
                        "/api/v1/auth/register/sms-code",
                        "/api/v1/auth/register",
                        "/api/v1/auth/login/password",
                        "/api/v1/auth/login/sms",
                        "/api/v1/auth/password/reset-sms",
                        "/api/v1/auth/password/reset",
                        "/api/v1/auth/captcha",
                        "/api/v1/auth/check-username"
                )
                .addPathPatterns("/**");
    }
}
