package com.xianggui.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private JwtProperties jwt = new JwtProperties();
    private CaptchaProperties captcha = new CaptchaProperties();
    private SecurityProperties security = new SecurityProperties();
    private CorsProperties cors = new CorsProperties();

    @Data
    public static class JwtProperties {
        private String secret;
        private String issuer = "xianggui-app";
        private Long accessTokenExpire = 604800L;
        private Long refreshTokenExpire = 2592000L;
        private Long registerTokenExpire = 604800L;
    }

    @Data
    public static class CaptchaProperties {
        private ImageCaptchaProperties image = new ImageCaptchaProperties();
        private SmsCaptchaProperties sms = new SmsCaptchaProperties();
    }

    @Data
    public static class ImageCaptchaProperties {
        private Integer expireSeconds = 300;
        private Integer length = 4;
        private Integer width = 120;
        private Integer height = 40;
    }

    @Data
    public static class SmsCaptchaProperties {
        private Integer expireSeconds = 300;
        private Integer length = 6;
        private Integer rateLimitSeconds = 60;
        private Integer dailyLimit = 10;
    }

    @Data
    public static class SecurityProperties {
        private LoginProperties login = new LoginProperties();
        private PasswordProperties password = new PasswordProperties();
        private UsernameProperties username = new UsernameProperties();
        private MobileProperties mobile = new MobileProperties();
    }

    @Data
    public static class LoginProperties {
        private Integer maxFailAttempts = 5;
        private Integer lockDurationMinutes = 30;
        private Integer failCountExpireMinutes = 60;
    }

    @Data
    public static class PasswordProperties {
        private Integer minLength = 6;
        private Integer maxLength = 20;
        private String pattern = "^[a-zA-Z0-9\\u4e00-\\u9fa5!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$";
    }

    @Data
    public static class UsernameProperties {
        private Integer minLength = 2;
        private Integer maxLength = 20;
        private String pattern = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$";
    }

    @Data
    public static class MobileProperties {
        private String pattern = "^1[3-9]\\d{9}$";
    }

    @Data
    public static class CorsProperties {
        private String allowedOrigins = "*";
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
        private String allowedHeaders = "*";
        private Boolean allowCredentials = false;
        private Long maxAge = 3600L;
    }
}
