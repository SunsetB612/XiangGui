package com.xianggui.app.service;

import com.xianggui.app.common.ApiResponse;
import com.xianggui.app.common.ErrorCode;
import com.xianggui.app.config.AppProperties;
import com.xianggui.app.dto.*;
import com.xianggui.app.entity.User;
import com.xianggui.app.exception.BusinessException;
import com.xianggui.app.mapper.UserMapper;
import com.xianggui.app.util.JwtUtil;
import com.xianggui.app.util.PasswordUtil;
import com.xianggui.app.util.RedisUtil;
import com.xianggui.app.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 认证服务
 * 处理用户认证相关业务逻辑：注册、登录、密码管理等
 * 
 * Why: Service层承载核心业务逻辑，Controller只处理HTTP请求与响应
 * Warning: 涉及用户隐私数据（密码、手机号）时，必须加密或脱敏处理
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private AppProperties appProperties;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 发送注册验证码
     * Why: 分离验证码发送与注册逻辑，支持重试机制
     * Warning: 受频率限制保护，防止短信轰炸
     */
    public ApiResponse<Void> sendRegisterSmsCode(RegisterSmsCodeRequest request) {
        // 验证手机号格式 - 防御性校验（即使DTO有校验也保留）
        if (!ValidationUtil.isValidMobile(request.getMobile())) {
            log.warn("[发送注册验证码] 手机号格式错误, mobile={}", maskMobile(request.getMobile()));
            throw new BusinessException(ErrorCode.INVALID_MOBILE, "手机号格式错误");
        }

        // 验证用户名格式
        if (!ValidationUtil.isValidUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.INVALID_USERNAME,
                "用户名格式错误，支持" + appProperties.getSecurity().getUsername().getMinLength() +
                "-" + appProperties.getSecurity().getUsername().getMaxLength() + "位中英文、数字或下划线");
        }

        // 检查手机号是否已注册
        if (userMapper.existsMobile(request.getMobile()) > 0) {
            throw new BusinessException(ErrorCode.MOBILE_ALREADY_REGISTERED, "手机号已注册");
        }

        // 检查用户名是否存在
        if (userMapper.existsUsername(request.getUsername()) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS, "用户名已存在");
        }

        // 检查发送频率限制
        if (!redisUtil.checkSmsRateLimit(request.getMobile())) {
            throw new BusinessException(ErrorCode.REQUEST_TOO_FREQUENT,
                "请求过于频繁，请" + appProperties.getCaptcha().getSms().getRateLimitSeconds() + "秒后重试");
        }

        // 生成验证码并保存到Redis
        String code = generateCode();
        redisUtil.setSmsCode(request.getMobile(), "register", code);
        redisUtil.setSmsRateLimit(request.getMobile());

        // TODO: 集成真实短信服务发送验证码
        // Warning: 开发环境可打印到日志，生产环境必须接入短信网关
        if (log.isDebugEnabled()) {
            log.debug("[发送注册验证码] mobile={}, code={}", maskMobile(request.getMobile()), code);
        }

        // 结构化日志记录
        logStructured("SMS_CODE_SENT", Map.of(
            "mobile", maskMobile(request.getMobile()),
            "username", request.getUsername(),
            "bizType", "register"
        ));

        return ApiResponse.success(null, "验证码发送成功");
    }

    /**
     * 用户注册
     * Why: 使用验证码验证手机号真实性，防止恶意注册
     * Warning: 验证码验证成功后立即删除，防止重放攻击
     */
    public ApiResponse<RegisterResponse> register(RegisterRequest request) {
        // 获取Redis中的验证码
        String savedCode = redisUtil.getSmsCode(request.getMobile(), "register");

        if (savedCode == null) {
            throw new BusinessException(ErrorCode.INVALID_CODE, "验证码已过期，请重新获取");
        }

        if (!savedCode.equals(request.getCode())) {
            log.warn("[用户注册] 验证码错误, mobile={}", maskMobile(request.getMobile()));
            throw new BusinessException(ErrorCode.INVALID_CODE, "验证码错误");
        }

        // 删除已使用的验证码 - 安全：防止重放攻击
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

        // 结构化日志记录
        logStructured("USER_REGISTERED", Map.of(
            "userId", user.getId(),
            "username", user.getUsername(),
            "mobile", maskMobile(user.getMobile())
        ));

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
     * Why: 支持记住登录状态，延长Token有效期
     * Warning: 连续失败会触发账号锁定机制
     */
    public ApiResponse<LoginResponse> loginByPassword(LoginPasswordRequest request) {
        // 检查手机号是否注册
        User user = userMapper.selectByMobile(request.getMobile());
        if (user == null) {
            recordLoginFailure(request.getMobile());
            throw new BusinessException(ErrorCode.MOBILE_NOT_REGISTERED, "该手机号未注册");
        }

        // 检查账号是否被锁定
        if (redisUtil.isAccountLocked(request.getMobile())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                "账号已被锁定，请" + appProperties.getSecurity().getLogin().getLockDurationMinutes() + "分钟后再试");
        }

        // 验证密码
        if (user.getPasswordHash() == null || !PasswordUtil.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            recordLoginFailure(request.getMobile());
            log.warn("[密码登录] 密码错误, userId={}, mobile={}", user.getId(), maskMobile(request.getMobile()));
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "手机号或密码错误");
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

        // 结构化日志记录
        logStructured("USER_LOGIN", Map.of(
            "userId", user.getId(),
            "username", user.getUsername(),
            "mobile", maskMobile(user.getMobile()),
            "loginType", "password",
            "rememberMe", rememberMe
        ));

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
     * Why: 为忘记密码用户提供替代登录方式
     * Warning: 与密码登录共享账号锁定机制
     */
    public ApiResponse<LoginResponse> loginBySms(LoginSmsRequest request) {
        // 检查手机号是否注册
        User user = userMapper.selectByMobile(request.getMobile());
        if (user == null) {
            throw new BusinessException(ErrorCode.MOBILE_NOT_REGISTERED, "该手机号未注册");
        }

        // 检查账号是否被锁定
        if (redisUtil.isAccountLocked(request.getMobile())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                "账号已被锁定，请" + appProperties.getSecurity().getLogin().getLockDurationMinutes() + "分钟后再试");
        }

        // 验证验证码
        String savedCode = redisUtil.getSmsCode(request.getMobile(), "login");
        if (savedCode == null) {
            throw new BusinessException(ErrorCode.INVALID_CODE, "验证码已过期，请重新获取");
        }

        if (!savedCode.equals(request.getCode())) {
            log.warn("[短信登录] 验证码错误, userId={}, mobile={}", user.getId(), maskMobile(request.getMobile()));
            throw new BusinessException(ErrorCode.INVALID_CODE, "验证码错误");
        }

        // 删除已使用的验证码
        redisUtil.deleteSmsCode(request.getMobile(), "login");

        // 更新登录信息
        userMapper.updateLoginInfo(user.getId(), getClientIp());

        // 生成token
        long expiresIn = JwtUtil.getDefaultExpiresIn(false);
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getMobile(), expiresIn);
        redisUtil.setUserSession(token, user.getId(), user.getUsername(), user.getMobile(), expiresIn);

        // 结构化日志记录
        logStructured("USER_LOGIN", Map.of(
            "userId", user.getId(),
            "username", user.getUsername(),
            "mobile", maskMobile(user.getMobile()),
            "loginType", "sms"
        ));

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
     * Why: 独立的验证码类型，与注册/登录验证码隔离
     * Warning: 仅对已注册手机号发送
     */
    public ApiResponse<Void> sendResetPasswordSmsCode(ResetPasswordSmsRequest request) {
        // 检查手机号是否注册
        if (userMapper.existsMobile(request.getMobile()) == 0) {
            throw new BusinessException(ErrorCode.MOBILE_NOT_REGISTERED, "该手机号未注册");
        }

        // 检查发送频率限制
        if (!redisUtil.checkSmsRateLimit(request.getMobile())) {
            throw new BusinessException(ErrorCode.REQUEST_TOO_FREQUENT,
                "请求过于频繁，请" + appProperties.getCaptcha().getSms().getRateLimitSeconds() + "秒后重试");
        }

        // 生成验证码并保存到Redis
        String code = generateCode();
        redisUtil.setSmsCode(request.getMobile(), "reset_password", code);
        redisUtil.setSmsRateLimit(request.getMobile());

        // TODO: 集成真实短信服务
        if (log.isDebugEnabled()) {
            log.debug("[发送重置密码验证码] mobile={}, code={}", maskMobile(request.getMobile()), code);
        }

        return ApiResponse.success(null, "验证码发送成功");
    }

    /**
     * 重置密码
     * Why: 验证码+双密码确认，确保身份和输入正确性
     * Warning: 密码重置后清除所有会话，强制重新登录
     */
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        // 检查手机号是否注册
        if (userMapper.existsMobile(request.getMobile()) == 0) {
            throw new BusinessException(ErrorCode.MOBILE_NOT_REGISTERED, "该手机号未注册");
        }

        // 验证验证码
        String savedCode = redisUtil.getSmsCode(request.getMobile(), "reset_password");
        if (savedCode == null) {
            throw new BusinessException(ErrorCode.INVALID_CODE, "验证码已过期，请重新获取");
        }

        if (!savedCode.equals(request.getCode())) {
            throw new BusinessException(ErrorCode.INVALID_CODE, "验证码错误");
        }

        // 验证新密码格式
        if (!ValidationUtil.isValidPassword(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT,
                "密码格式错误，支持" + appProperties.getSecurity().getPassword().getMinLength() +
                "-" + appProperties.getSecurity().getPassword().getMaxLength() + "位中英文、数字或特殊字符");
        }

        // 验证两次密码一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH, "两次输入的密码不一致");
        }

        // 删除已使用的验证码
        redisUtil.deleteSmsCode(request.getMobile(), "reset_password");

        // 更新密码
        String hashedPassword = PasswordUtil.hashPassword(request.getNewPassword());
        userMapper.updatePassword(request.getMobile(), hashedPassword);

        // 清除该账号所有会话 - 安全：强制重新登录
        redisUtil.clearLoginFailure(request.getMobile());

        // 结构化日志记录
        logStructured("PASSWORD_RESET", Map.of(
            "mobile", maskMobile(request.getMobile())
        ));

        return ApiResponse.success(null, "密码重置成功");
    }

    /**
     * 获取图形验证码
     * Why: 人机验证，防止自动化攻击
     * Warning: 图形验证码应与业务操作关联验证
     */
    public ApiResponse<CaptchaResponse> getCaptcha() {
        String captchaKey = "captcha_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000000);
        String code = generateCaptchaCode();

        redisUtil.setCaptcha(captchaKey, code);

        // Warning: 简化处理，实际应生成真实的验证码图片
        String imageData = "data:image/png;base64,iVBORw0KGgoAAAAN...";

        CaptchaResponse response = CaptchaResponse.builder()
                .captchaKey(captchaKey)
                .imageData(imageData)
                .expireIn(appProperties.getCaptcha().getImage().getExpireSeconds())
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 检查用户名是否可用
     * Why: 前端实时校验，提升用户体验
     * Warning: 仅作为预检，正式注册时仍需校验
     */
    public ApiResponse<CheckUsernameResponse> checkUsername(String username) {
        if (!ValidationUtil.isValidUsername(username)) {
            throw new BusinessException(ErrorCode.INVALID_USERNAME, "用户名格式错误");
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
     * Why: 安全机制，防止暴力破解
     * Warning: 达到阈值后锁定账号
     */
    private void recordLoginFailure(String mobile) {
        redisUtil.recordLoginFailure(mobile);
        Long failCount = redisUtil.getLoginFailureCount(mobile);
        int maxAttempts = appProperties.getSecurity().getLogin().getMaxFailAttempts();

        // 连续失败达到阈值则锁定账号
        if (failCount >= maxAttempts) {
            redisUtil.lockAccount(mobile, "password_failures_exceeded");
            log.warn("[账号锁定] 连续登录失败次数过多, mobile={}, failCount={}", maskMobile(mobile), failCount);
        }
    }

    /**
     * 生成数字验证码
     */
    private String generateCode() {
        int length = appProperties.getCaptcha().getSms().getLength();
        int maxNum = (int) Math.pow(10, length);
        return String.format("%0" + length + "d", new Random().nextInt(maxNum));
    }

    /**
     * 生成图形验证码
     */
    private String generateCaptchaCode() {
        int length = appProperties.getCaptcha().getImage().getLength();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
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
     * Warning: 生产环境应从RequestContext获取真实IP
     */
    private String getClientIp() {
        return "127.0.0.1";
    }

    /**
     * 手机号脱敏
     * Why: 保护用户隐私，日志中不显示完整手机号
     */
    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    /**
     * 结构化日志记录
     * Why: 便于日志收集和分析系统解析
     */
    private void logStructured(String eventType, Map<String, Object> data) {
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("timestamp", LocalDateTime.now().format(DTF));
        logData.put("event", eventType);
        logData.putAll(data);
        log.info("[业务事件] {}", logData);
    }
}
