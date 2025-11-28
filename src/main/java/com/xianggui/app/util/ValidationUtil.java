package com.xianggui.app.util;

import java.util.regex.Pattern;

public class ValidationUtil {
    private static final String MOBILE_PATTERN = "^1[3-9]\\d{9}$";
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]{2,20}$";
    private static final String PASSWORD_PATTERN = "^[a-zA-Z0-9!@#$%^&*_-]{6,20}$";

    public static boolean isValidMobile(String mobile) {
        if (mobile == null) {
            return false;
        }
        return Pattern.matches(MOBILE_PATTERN, mobile);
    }

    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        return Pattern.matches(USERNAME_PATTERN, username);
    }

    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        return Pattern.matches(PASSWORD_PATTERN, password);
    }

    public static boolean isValidCode(String code) {
        return code != null && code.matches("^\\d{6}$");
    }
}
