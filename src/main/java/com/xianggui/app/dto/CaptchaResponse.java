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
public class CaptchaResponse {
    @JsonProperty("captcha_key")
    private String captchaKey;
    @JsonProperty("image_data")
    private String imageData;
    @JsonProperty("expire_in")
    private Integer expireIn;
}
