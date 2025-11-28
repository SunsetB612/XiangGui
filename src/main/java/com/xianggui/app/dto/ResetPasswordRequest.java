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
public class ResetPasswordRequest {
    private String mobile;
    private String code;
    @JsonProperty("new_password")
    private String newPassword;
    @JsonProperty("confirm_password")
    private String confirmPassword;
}
