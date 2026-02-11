package com.xianggui.app.common;

public class ErrorCode {
    // 通用错误
    public static final Integer INTERNAL_ERROR = 5000;
    public static final Integer PARAM_ERROR = 4000;

    // 手机号格式错误
    public static final Integer INVALID_MOBILE = 4001;
    // 用户名格式错误
    public static final Integer INVALID_USERNAME = 4002;
    // 手机号已注册
    public static final Integer MOBILE_ALREADY_REGISTERED = 4003;
    // 用户名已存在
    public static final Integer USERNAME_ALREADY_EXISTS = 4004;
    // 请求过于频繁
    public static final Integer REQUEST_TOO_FREQUENT = 4301;
    // 验证码错误
    public static final Integer INVALID_CODE = 4101;
    // 验证码已过期
    public static final Integer CODE_EXPIRED = 4102;
    // 手机号或密码错误
    public static final Integer INVALID_CREDENTIALS = 4201;
    // 账号已被锁定
    public static final Integer ACCOUNT_LOCKED = 4202;
    // 手机号未注册
    public static final Integer MOBILE_NOT_REGISTERED = 4203;
    // 密码格式错误
    public static final Integer INVALID_PASSWORD_FORMAT = 4401;
    // 密码不一致
    public static final Integer PASSWORD_MISMATCH = 4402;
}
