package com.xianggui.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {
    // JWT密钥（至少256位用于HS256算法）
    private static final String SECRET_KEY = "xianggui_2024_jwt_secret_key_with_minimum_256_bits_length_for_hs256_algorithm_security";

    @Bean
    public SecretKey secretKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    public String jwtSecret() {
        return SECRET_KEY;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
