package com.xianggui.app.controller;

import com.xianggui.app.common.ApiResponse;
import com.xianggui.app.dto.*;
import com.xianggui.app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 处理用户认证相关HTTP请求：注册、登录、密码管理等
 * 
 * Why: 遵循Controller层只处理HTTP请求与响应，业务逻辑下沉至Service层
 * Warning: 所有接口入参均经过@Valid校验，拒绝信任前端输入
 */
@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "认证管理", description = "用户认证相关接口：注册、登录、密码管理等")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 发送注册验证码
     * Why: 分离验证码发送与注册逻辑，支持重试机制
     * Warning: 受频率限制保护，防止短信轰炸
     */
    @PostMapping("/register/sms-code")
    @Operation(summary = "发送注册验证码", description = "向指定手机号发送注册短信验证码，同时校验用户名是否可用")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "验证码发送成功")
    public ApiResponse<Void> sendRegisterSmsCode(
            @Valid @RequestBody RegisterSmsCodeRequest request) {
        return authService.sendRegisterSmsCode(request);
    }

    /**
     * 用户注册
     * Why: 使用验证码验证手机号真实性，防止恶意注册
     * Warning: 验证码验证成功后立即删除，防止重放攻击
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "使用手机号和验证码完成用户注册，返回登录凭证")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功")
    public ApiResponse<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * 密码登录
     * Why: 支持记住登录状态，延长Token有效期
     * Warning: 连续失败会触发账号锁定机制
     */
    @PostMapping("/login/password")
    @Operation(summary = "密码登录", description = "使用手机号和密码进行登录，支持记住登录状态")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功")
    public ApiResponse<LoginResponse> loginByPassword(
            @Valid @RequestBody LoginPasswordRequest request) {
        return authService.loginByPassword(request);
    }

    /**
     * 短信验证码登录
     * Why: 为忘记密码用户提供替代登录方式
     * Warning: 与密码登录共享账号锁定机制
     */
    @PostMapping("/login/sms")
    @Operation(summary = "短信验证码登录", description = "使用手机号和短信验证码进行登录")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功")
    public ApiResponse<LoginResponse> loginBySms(
            @Valid @RequestBody LoginSmsRequest request) {
        return authService.loginBySms(request);
    }

    /**
     * 发送重置密码验证码
     * Why: 独立的验证码类型，与注册/登录验证码隔离
     * Warning: 仅对已注册手机号发送
     */
    @PostMapping("/password/reset-sms")
    @Operation(summary = "发送重置密码验证码", description = "向已注册手机号发送密码重置验证码")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "验证码发送成功")
    public ApiResponse<Void> sendResetPasswordSmsCode(
            @Valid @RequestBody ResetPasswordSmsRequest request) {
        return authService.sendResetPasswordSmsCode(request);
    }

    /**
     * 重置密码
     * Why: 验证码+双密码确认，确保身份和输入正确性
     * Warning: 密码重置后清除所有会话，强制重新登录
     */
    @PostMapping("/password/reset")
    @Operation(summary = "重置密码", description = "使用验证码验证身份后重置密码")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "密码重置成功")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    /**
     * 获取图形验证码
     * Why: 人机验证，防止自动化攻击
     * Warning: 图形验证码应与业务操作关联验证
     */
    @GetMapping("/captcha")
    @Operation(summary = "获取图形验证码", description = "获取图形验证码用于人机验证")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    public ApiResponse<CaptchaResponse> getCaptcha() {
        return authService.getCaptcha();
    }

    /**
     * 检查用户名是否可用
     * Why: 前端实时校验，提升用户体验
     * Warning: 仅作为预检，正式注册时仍需校验
     */
    @GetMapping("/check-username")
    @Operation(summary = "检查用户名是否可用", description = "检查指定用户名是否已被注册，如已被占用则返回建议用户名")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "检查完成")
    public ApiResponse<CheckUsernameResponse> checkUsername(
            @Parameter(description = "要检查的用户名", required = true, example = "test_user")
            @NotBlank(message = "用户名不能为空")
            @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$", message = "用户名格式错误")
            @RequestParam String username) {
        return authService.checkUsername(username);
    }
}
