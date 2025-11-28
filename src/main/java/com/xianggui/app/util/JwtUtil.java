package com.xianggui.app.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    // JWT密钥（至少256位用于HS256算法）
    private static final String SECRET_KEY = "xianggui_2024_jwt_secret_key_with_minimum_256_bits_length_for_hs256_algorithm_security";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    /**
     * 生成JWT Token
     */
    public static String generateToken(Long userId, String username, String mobile, long expiresIn) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userId);
        claims.put("username", username);
        claims.put("mobile", mobile);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiresIn * 1000);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证Token并获取声明
     */
    public static Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从Token中获取用户ID
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return claims.get("user_id", Long.class);
        }
        return null;
    }

    /**
     * 从Token中获取用户名
     */
    public static String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return claims.getSubject();
        }
        return null;
    }

    /**
     * 从Token中获取手机号
     */
    public static String getMobileFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return (String) claims.get("mobile");
        }
        return null;
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
