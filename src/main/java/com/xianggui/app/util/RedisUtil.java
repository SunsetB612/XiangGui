package com.xianggui.app.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xianggui.app.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    @Autowired
    public RedisUtil(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, AppProperties appProperties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    /**
     * 存储短信验证码
     */
    public void setSmsCode(String mobile, String codeType, String code) {
        String key = "sms:code:" + mobile + ":" + codeType;
        long expireSeconds = appProperties.getCaptcha().getSms().getExpireSeconds();
        redisTemplate.opsForValue().set(key, code, expireSeconds, TimeUnit.SECONDS);
    }

    /**
     * 获取短信验证码
     */
    public String getSmsCode(String mobile, String codeType) {
        String key = "sms:code:" + mobile + ":" + codeType;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除短信验证码
     */
    public void deleteSmsCode(String mobile, String codeType) {
        String key = "sms:code:" + mobile + ":" + codeType;
        redisTemplate.delete(key);
    }

    /**
     * 检查发送频率限制
     */
    public boolean checkSmsRateLimit(String mobile) {
        String key = "sms:rate:limit:" + mobile;
        return !redisTemplate.hasKey(key);
    }

    /**
     * 设置发送频率限制
     */
    public void setSmsRateLimit(String mobile) {
        String key = "sms:rate:limit:" + mobile;
        long rateLimitSeconds = appProperties.getCaptcha().getSms().getRateLimitSeconds();
        redisTemplate.opsForValue().set(key, System.currentTimeMillis() + "", rateLimitSeconds, TimeUnit.SECONDS);
    }

    /**
     * 记录登录失败次数
     */
    public void recordLoginFailure(String mobile) {
        String key = "login:fail:count:" + mobile;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            long expireMinutes = appProperties.getSecurity().getLogin().getFailCountExpireMinutes();
            redisTemplate.expire(key, expireMinutes, TimeUnit.MINUTES);
        }
    }

    /**
     * 获取登录失败次数
     */
    public Long getLoginFailureCount(String mobile) {
        String key = "login:fail:count:" + mobile;
        String count = redisTemplate.opsForValue().get(key);
        return count == null ? 0L : Long.parseLong(count);
    }

    /**
     * 清除登录失败次数
     */
    public void clearLoginFailure(String mobile) {
        String key = "login:fail:count:" + mobile;
        redisTemplate.delete(key);
    }

    /**
     * 锁定账号
     */
    public void lockAccount(String mobile, String reason) {
        String key = "login:lock:" + mobile;
        long lockDurationMinutes = appProperties.getSecurity().getLogin().getLockDurationMinutes();
        Map<String, Object> lockInfo = Map.of(
            "lock_until", System.currentTimeMillis() + lockDurationMinutes * 60 * 1000,
            "reason", reason,
            "fail_count", appProperties.getSecurity().getLogin().getMaxFailAttempts()
        );
        try {
            String value = objectMapper.writeValueAsString(lockInfo);
            redisTemplate.opsForValue().set(key, value, lockDurationMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Warning: 生产环境应使用日志框架
            e.printStackTrace();
        }
    }

    /**
     * 检查账号是否被锁定
     */
    public boolean isAccountLocked(String mobile) {
        String key = "login:lock:" + mobile;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 解锁账号
     */
    public void unlockAccount(String mobile) {
        String key = "login:lock:" + mobile;
        redisTemplate.delete(key);
    }

    /**
     * 保存图形验证码
     */
    public void setCaptcha(String captchaKey, String code) {
        String key = "captcha:" + captchaKey;
        long expireSeconds = appProperties.getCaptcha().getImage().getExpireSeconds();
        redisTemplate.opsForValue().set(key, code, expireSeconds, TimeUnit.SECONDS);
    }

    /**
     * 获取图形验证码
     */
    public String getCaptcha(String captchaKey) {
        String key = "captcha:" + captchaKey;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 保存用户会话token
     */
    public void setUserSession(String token, Long userId, String username, String mobile, long expiresIn) {
        String key = "session:token:" + token;
        Map<String, Object> sessionData = Map.of(
            "user_id", userId,
            "username", username,
            "mobile", mobile
        );
        try {
            String value = objectMapper.writeValueAsString(sessionData);
            redisTemplate.opsForValue().set(key, value, expiresIn, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取用户会话
     */
    public String getUserSession(String token) {
        String key = "session:token:" + token;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除用户会话
     */
    public void deleteUserSession(String token) {
        String key = "session:token:" + token;
        redisTemplate.delete(key);
    }
}
