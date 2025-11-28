package com.xianggui.app.service;

import com.xianggui.app.common.ApiResponse;
import com.xianggui.app.common.ErrorCode;
import com.xianggui.app.dto.*;
import com.xianggui.app.entity.User;
import com.xianggui.app.mapper.UserMapper;
import com.xianggui.app.util.JwtUtil;
import com.xianggui.app.util.PasswordUtil;
import com.xianggui.app.util.RedisUtil;
import com.xianggui.app.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class AuthService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 发送注册验证码
     */
    public ApiResponse<Void> sendRegisterSmsCode(RegisterSmsCodeRequest request) {
        // 验证手机号格式
        if (!ValidationUtil.isValidMobile(request.getMobile())) {
            return ApiResponse.error(ErrorCode.INVALID_MOBILE, "手机号格式错误");
        }

        // 验证用户名格式
        if (!ValidationUtil.isValidUsername(request.getUsername())) {
            return ApiResponse.error(ErrorCode.INVALID_USERNAME, "用户名格式错误，支持2-20位中英文、数字或下划线");
        }

        // 检查手机号是否已注册
        if (userMapper.existsMobile(request.getMobile()) > 0) {
            return ApiResponse.error(ErrorCode.MOBILE_ALREADY_REGISTERED, "手机号已注册");
        }

        // 检查用户名是否存在
        if (userMapper.existsUsername(request.getUsername()) > 0) {
            return ApiResponse.error(ErrorCode.USERNAME_ALREADY_EXISTS, "用户名已存在");
        }

        // 检查发送频率限制
        if (!redisUtil.checkSmsRateLimit(request.getMobile())) {
            return ApiResponse.error(ErrorCode.REQUEST_TOO_FREQUENT, "请求过于频繁，请60秒后重试");
        }

        // 生成验证码并保存到Redis
        String code = generateCode();
        redisUtil.setSmsCode(request.getMobile(), "register", code);
        redisUtil.setSmsRateLimit(request.getMobile());

        return ApiResponse.success(null, "验证码发送成功");
    }

    /**
     * 用户注册
     */
    public ApiResponse<RegisterResponse> register(RegisterRequest request) {
        // 获取Redis中的验证码
        String savedCode = redisUtil.getSmsCode(request.getMobile(), "register");

        if (savedCode == null) {
            return ApiResponse.error(ErrorCode.INVALID_CODE, "验证码错误");
        }

        if (!savedCode.equals(request.getCode())) {
            return ApiResponse.error(ErrorCode.INVALID_CODE, "验证码错误");
        }

        // 删除已使用的验证码
        redisUtil.deleteSmsCode(request.getMobile(), "register");

        // 创建用户（暂不设置密码，需创建虚拟形象后再设置）
        User user = User.builder()
                .username(request.getUsername())
                .mobile(request.getMobile())
                .userStatus(2) // 未完成注册
                .build();

        userMapper.insert(user);

        // 生成token
        long expiresIn = JwtUtil.getRegisterExpiresIn();
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getMobile(), expiresIn);
        redisUtil.setUserSession(token, user.getId(), user.getUsername(), user.getMobile(), expiresIn);

        RegisterResponse response = RegisterResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .needCreateAvatar(true)
                .build();

        return ApiResponse.success(response, "注册成功");
    }

    /**
     * 密码登录
     */
    public ApiResponse<LoginResponse> loginByPassword(LoginPasswordRequest request) {
        // 检查手机号是否注册
        User user = userMapper.selectByMobile(request.getMobile());
        if (user == null) {
            recordLoginFailure(request.getMobile());
            return ApiResponse.error(ErrorCode.MOBILE_NOT_REGISTERED, "该手机号未注册");
        }

        // 检查账号是否被锁定
        if (redisUtil.isAccountLocked(request.getMobile())) {
            return ApiResponse.error(ErrorCode.ACCOUNT_LOCKED, "账号已被锁定，请30分钟后再试");
        }

        // 验证密码
        if (user.getPasswordHash() == null || !PasswordUtil.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            recordLoginFailure(request.getMobile());
            return ApiResponse.error(ErrorCode.INVALID_CREDENTIALS, "手机号或密码错误");
        }

        // 清除登录失败记录
        redisUtil.clearLoginFailure(request.getMobile());

        // 更新登录信息
        userMapper.updateLoginInfo(user.getId(), getClientIp());

        // 生成token
        boolean rememberMe = request.getRememberMe() != null && request.getRememberMe();
        long expiresIn = JwtUtil.getDefaultExpiresIn(rememberMe);
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getMobile(), expiresIn);
        redisUtil.setUserSession(token, user.getId(), user.getUsername(), user.getMobile(), expiresIn);

        LoginResponse response = LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .mobile(user.getMobile())
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .avatarCreated(user.getAvatarConfig() != null)
                .build();

        return ApiResponse.success(response, "登录成功");
    }

    /**
     * 短信验证码登录
     */
    public ApiResponse<LoginResponse> loginBySms(LoginSmsRequest request) {
        // 检查手机号是否注册
        User user = userMapper.selectByMobile(request.getMobile());
        if (user == null) {
            return ApiResponse.error(ErrorCode.MOBILE_NOT_REGISTERED, "该手机号未注册");
        }

        // 检查账号是否被锁定
        if (redisUtil.isAccountLocked(request.getMobile())) {
            return ApiResponse.error(ErrorCode.ACCOUNT_LOCKED, "账号已被锁定，请30分钟后再试");
        }

        // 验证验证码
        String savedCode = redisUtil.getSmsCode(request.getMobile(), "login");
        if (savedCode == null) {
            return ApiResponse.error(ErrorCode.INVALID_CODE, "验证码错误");
        }

        if (!savedCode.equals(request.getCode())) {
            return ApiResponse.error(ErrorCode.INVALID_CODE, "验证码错误");
        }

        // 删除已使用的验证码
        redisUtil.deleteSmsCode(request.getMobile(), "login");

        // 更新登录信息
        userMapper.updateLoginInfo(user.getId(), getClientIp());

        // 生成token
        long expiresIn = JwtUtil.getDefaultExpiresIn(false);
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getMobile(), expiresIn);
        redisUtil.setUserSession(token, user.getId(), user.getUsername(), user.getMobile(), expiresIn);

        LoginResponse response = LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .mobile(user.getMobile())
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .avatarCreated(user.getAvatarConfig() != null)
                .build();

        return ApiResponse.success(response, "登录成功");
    }

    /**
     * 发送重置密码验证码
     */
    public ApiResponse<Void> sendResetPasswordSmsCode(ResetPasswordSmsRequest request) {
        // 检查手机号是否注册
        if (userMapper.existsMobile(request.getMobile()) == 0) {
            return ApiResponse.error(ErrorCode.MOBILE_NOT_REGISTERED, "该手机号未注册");
        }

        // 检查发送频率限制
        if (!redisUtil.checkSmsRateLimit(request.getMobile())) {
            return ApiResponse.error(ErrorCode.REQUEST_TOO_FREQUENT, "请求过于频繁，请60秒后重试");
        }

        // 生成验证码并保存到Redis
        String code = generateCode();
        redisUtil.setSmsCode(request.getMobile(), "reset_password", code);
        redisUtil.setSmsRateLimit(request.getMobile());

        return ApiResponse.success(null, "验证码发送成功");
    }

    /**
     * 重置密码
     */
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        // 检查手机号是否注册
        if (userMapper.existsMobile(request.getMobile()) == 0) {
            return ApiResponse.error(ErrorCode.MOBILE_NOT_REGISTERED, "该手机号未注册");
        }

        // 验证验证码
        String savedCode = redisUtil.getSmsCode(request.getMobile(), "reset_password");
        if (savedCode == null) {
            return ApiResponse.error(ErrorCode.INVALID_CODE, "验证码错误");
        }

        if (!savedCode.equals(request.getCode())) {
            return ApiResponse.error(ErrorCode.INVALID_CODE, "验证码错误");
        }

        // 验证新密码格式
        if (!ValidationUtil.isValidPassword(request.getNewPassword())) {
            return ApiResponse.error(ErrorCode.INVALID_PASSWORD_FORMAT, "密码格式错误，支持6-20位中英文、数字或特殊字符");
        }

        // 验证两次密码一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.error(ErrorCode.PASSWORD_MISMATCH, "两次输入的密码不一致");
        }

        // 删除已使用的验证码
        redisUtil.deleteSmsCode(request.getMobile(), "reset_password");

        // 更新密码
        String hashedPassword = PasswordUtil.hashPassword(request.getNewPassword());
        userMapper.updatePassword(request.getMobile(), hashedPassword);

        // 清除该账号所有会话
        redisUtil.clearLoginFailure(request.getMobile());

        return ApiResponse.success(null, "密码重置成功");
    }

    /**
     * 获取图形验证码
     */
    public ApiResponse<CaptchaResponse> getCaptcha() {
        String captchaKey = "captcha_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000000);
        String code = generateCaptchaCode();
        
        redisUtil.setCaptcha(captchaKey, code);

        // 简化处理，实际应该生成真实的验证码图片
        String imageData = "data:image/png;base64,iVBORw0KGgoAAAAN...";

        CaptchaResponse response = CaptchaResponse.builder()
                .captchaKey(captchaKey)
                .imageData(imageData)
                .expireIn(300)
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 检查用户名是否可用
     */
    public ApiResponse<CheckUsernameResponse> checkUsername(String username) {
        if (!ValidationUtil.isValidUsername(username)) {
            return ApiResponse.error(400, "用户名格式错误");
        }

        boolean available = userMapper.existsUsername(username) == 0;

        CheckUsernameResponse response = CheckUsernameResponse.builder()
                .available(available)
                .suggestions(available ? null : generateSuggestions(username))
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 记录登录失败次数
     */
    private void recordLoginFailure(String mobile) {
        redisUtil.recordLoginFailure(mobile);
        Long failCount = redisUtil.getLoginFailureCount(mobile);
        
        // 连续失败5次锁定账号30分钟
        if (failCount >= 5) {
            redisUtil.lockAccount(mobile, "password_failures_exceeded");
        }
    }

    /**
     * 生成6位数验证码
     */
    private String generateCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    /**
     * 生成4位图形验证码
     */
    private String generateCaptchaCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 生成用户名建议
     */
    private List<String> generateSuggestions(String username) {
        List<String> suggestions = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            suggestions.add(username + i);
        }
        return suggestions;
    }

    /**
     * 获取客户端IP（简化处理）
     */
    private String getClientIp() {
        return "127.0.0.1";
    }
}
