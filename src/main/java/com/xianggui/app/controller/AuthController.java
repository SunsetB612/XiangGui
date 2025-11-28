package com.xianggui.app.controller;

import com.xianggui.app.common.ApiResponse;
import com.xianggui.app.dto.*;
import com.xianggui.app.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    /**
     * 发送注册验证码
     */
    @PostMapping("/register/sms-code")
    public ApiResponse<Void> sendRegisterSmsCode(@RequestBody RegisterSmsCodeRequest request) {
        return authService.sendRegisterSmsCode(request);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * 密码登录
     */
    @PostMapping("/login/password")
    public ApiResponse<LoginResponse> loginByPassword(@RequestBody LoginPasswordRequest request) {
        return authService.loginByPassword(request);
    }

    /**
     * 短信验证码登录
     */
    @PostMapping("/login/sms")
    public ApiResponse<LoginResponse> loginBySms(@RequestBody LoginSmsRequest request) {
        return authService.loginBySms(request);
    }

    /**
     * 发送重置密码验证码
     */
    @PostMapping("/password/reset-sms")
    public ApiResponse<Void> sendResetPasswordSmsCode(@RequestBody ResetPasswordSmsRequest request) {
        return authService.sendResetPasswordSmsCode(request);
    }

    /**
     * 重置密码
     */
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    /**
     * 获取图形验证码
     */
    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> getCaptcha() {
        return authService.getCaptcha();
    }

    /**
     * 检查用户名是否可用
     */
    @GetMapping("/check-username")
    public ApiResponse<CheckUsernameResponse> checkUsername(@RequestParam String username) {
        return authService.checkUsername(username);
    }
}
