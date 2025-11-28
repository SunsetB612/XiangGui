package com.xianggui.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginPasswordRequest {
    private String mobile;
    private String password;
    @JsonProperty("remember_me")
    private Boolean rememberMe;
}
