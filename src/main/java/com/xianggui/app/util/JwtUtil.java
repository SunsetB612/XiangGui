package com.xianggui.app.util;

import com.xianggui.app.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class JwtUtil {

    private static AppProperties appProperties;

    @Autowired
    public void setAppProperties(AppProperties properties) {
        JwtUtil.appProperties = properties;
    }

    /**
     * 生成JWT Token
     */
    public static String generateToken(Long userId, String username, String mobile, long expiresIn) {
        long now = System.currentTimeMillis();
        long exp = now + expiresIn * 1000;

        String payload = String.format(
            "{\"user_id\":%d,\"username\":\"%s\",\"mobile\":\"%s\",\"iat\":%d,\"exp\":%d,\"iss\":\"%s\"}",
            userId, username, mobile, now / 1000, exp / 1000, appProperties.getJwt().getIssuer()
        );

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

        String headerEncoded = base64UrlEncode(header);
        String payloadEncoded = base64UrlEncode(payload);

        String signature = hmacSha256(headerEncoded + "." + payloadEncoded, appProperties.getJwt().getSecret());

        return headerEncoded + "." + payloadEncoded + "." + signature;
    }

    /**
     * 验证并解析Token
     */
    public static TokenInfo parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            // 验证签名
            String expectedSignature = hmacSha256(parts[0] + "." + parts[1], appProperties.getJwt().getSecret());
            if (!expectedSignature.equals(parts[2])) {
                return null;
            }

            // 解析payload
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            // 简单解析（实际项目中建议使用JSON库）
            TokenInfo info = new TokenInfo();
            info.userId = extractLong(payloadJson, "user_id");
            info.username = extractString(payloadJson, "username");
            info.mobile = extractString(payloadJson, "mobile");
            info.expireAt = extractLong(payloadJson, "exp") * 1000;

            // 检查过期
            if (System.currentTimeMillis() > info.expireAt) {
                return null;
            }

            return info;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取默认token过期时间（单位：秒）
     */
    public static long getDefaultExpiresIn(Boolean rememberMe) {
        if (rememberMe != null && rememberMe) {
            return appProperties.getJwt().getRefreshTokenExpire();
        }
        return appProperties.getJwt().getAccessTokenExpire();
    }

    /**
     * 获取注册token过期时间（单位：秒）
     */
    public static long getRegisterExpiresIn() {
        return appProperties.getJwt().getRegisterTokenExpire();
    }

    private static String base64UrlEncode(String input) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC", e);
        }
    }

    private static Long extractLong(String json, String key) {
        String pattern = "\"" + key + "\":(\\d+)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }

    private static String extractString(String json, String key) {
        String pattern = "\"" + key + "\":\"([^\"]+)\"";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static class TokenInfo {
        public Long userId;
        public String username;
        public String mobile;
        public Long expireAt;
    }
}
