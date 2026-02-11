package com.xianggui.app.util;

import com.xianggui.app.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ValidationUtil {

    private static AppProperties appProperties;

    @Autowired
    public void setAppProperties(AppProperties properties) {
        ValidationUtil.appProperties = properties;
    }

    /**
     * 验证手机号格式
     */
    public static boolean isValidMobile(String mobile) {
        if (mobile == null || appProperties == null) {
            return false;
        }
        String pattern = appProperties.getSecurity().getMobile().getPattern();
        return Pattern.matches(pattern, mobile);
    }

    /**
     * 验证用户名格式
     */
    public static boolean isValidUsername(String username) {
        if (username == null || appProperties == null) {
            return false;
        }
        String pattern = appProperties.getSecurity().getUsername().getPattern();
        int minLength = appProperties.getSecurity().getUsername().getMinLength();
        int maxLength = appProperties.getSecurity().getUsername().getMaxLength();

        if (username.length() < minLength || username.length() > maxLength) {
            return false;
        }
        return Pattern.matches(pattern, username);
    }

    /**
     * 验证密码格式
     */
    public static boolean isValidPassword(String password) {
        if (password == null || appProperties == null) {
            return false;
        }
        String pattern = appProperties.getSecurity().getPassword().getPattern();
        int minLength = appProperties.getSecurity().getPassword().getMinLength();
        int maxLength = appProperties.getSecurity().getPassword().getMaxLength();

        if (password.length() < minLength || password.length() > maxLength) {
            return false;
        }
        return Pattern.matches(pattern, password);
    }

    /**
     * 验证短信验证码格式
     */
    public static boolean isValidCode(String code) {
        if (code == null || appProperties == null) {
            return false;
        }
        int length = appProperties.getCaptcha().getSms().getLength();
        String pattern = "^\\d{" + length + "}$";
        return code.matches(pattern);
    }

    /**
     * 验证图形验证码格式
     */
    public static boolean isValidCaptchaCode(String code) {
        if (code == null || appProperties == null) {
            return false;
        }
        int length = appProperties.getCaptcha().getImage().getLength();
        return code.length() == length;
    }
}
