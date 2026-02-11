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
@Schema(description = "图形验证码响应")
public class CaptchaResponse {

    @Schema(description = "验证码标识", example = "captcha_1234567890")
    private String captchaKey;

    @Schema(description = "验证码图片Base64", example = "data:image/png;base64,iVBORw0KGgoAAAAN...")
    private String imageData;

    @Schema(description = "过期时间（秒）", example = "300")
    private Integer expireIn;
}
