package com.xianggui.app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AppProperties appProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        AppProperties.CorsProperties cors = appProperties.getCors();
        registry.addMapping("/**")
                .allowedOrigins(cors.getAllowedOrigins().split(","))
                .allowedMethods(cors.getAllowedMethods().split(","))
                .allowedHeaders(cors.getAllowedHeaders().split(","))
                .allowCredentials(cors.getAllowCredentials())
                .maxAge(cors.getMaxAge());
    }
}
