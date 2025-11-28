package com.xianggui.app.interceptor;

import com.xianggui.app.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 无token的请求继续放行，由具体的接口决定是否需要认证
            return true;
        }

        String token = authHeader.substring(7);
        Claims claims = JwtUtil.validateToken(token);

        if (claims == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Invalid or expired token\"}");
            return false;
        }

        // 将用户信息存入request属性，供后续使用
        request.setAttribute("user_id", claims.get("user_id"));
        request.setAttribute("username", claims.getSubject());
        request.setAttribute("mobile", claims.get("mobile"));

        return true;
    }
}
