package com.xianggui.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    @JsonProperty("user_id")
    private Long userId;
    private String username;
    private String mobile;
    private String token;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private Long expiresIn;
    @JsonProperty("avatar_created")
    private Boolean avatarCreated;
    @JsonProperty("avatar_config")
    private Map<String, String> avatarConfig;
}
