package com.xianggui.app.util;

import java.util.Base64;

public class JwtUtil {
    private static final String SECRET = "xianggui_secret_key_2024";

    /**
     * 生成JWT Token
     */
    public static String generateToken(Long userId, String username, String mobile, long expiresIn) {
        long now = System.currentTimeMillis();
        long exp = now + expiresIn * 1000;
        
        // 简化实现：只做base64编码，实际应使用真正的JWT库
        String payload = String.format(
            "{\"user_id\":%d,\"username\":\"%s\",\"mobile\":\"%s\",\"iat\":%d,\"exp\":%d}",
            userId, username, mobile, now / 1000, exp / 1000
        );
        
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        
        String headerEncoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(header.getBytes());
        String payloadEncoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes());
        
        // 简化签名
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((headerEncoded + payloadEncoded + SECRET).getBytes());
        
        return headerEncoded + "." + payloadEncoded + "." + signature;
    }

    /**
     * 获取token过期时间（单位：秒）
     */
    public static long getDefaultExpiresIn(Boolean rememberMe) {
        if (rememberMe != null && rememberMe) {
            return 30 * 24 * 60 * 60L; // 30天
        }
        return 7 * 24 * 60 * 60L; // 7天
    }

    /**
     * 获取注册token过期时间（单位：秒）
     */
    public static long getRegisterExpiresIn() {
        return 7 * 24 * 60 * 60L; // 7天
    }
}
