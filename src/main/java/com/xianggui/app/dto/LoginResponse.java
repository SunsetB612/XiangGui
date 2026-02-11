package com.xianggui.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "用户ID", example = "12345")
    private Long userId;

    @Schema(description = "用户名", example = "test_user")
    private String username;

    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiIs...")
    private String token;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "过期时间（秒）", example = "604800")
    private Long expiresIn;

    @Schema(description = "是否已创建虚拟形象", example = "true")
    private Boolean avatarCreated;
}
