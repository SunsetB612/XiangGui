package com.xianggui.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "检查用户名响应")
public class CheckUsernameResponse {

    @Schema(description = "是否可用", example = "true")
    private Boolean available;

    @Schema(description = "建议用户名列表（当用户名不可用时返回）", example = "[\"test_user1\", \"test_user2\"]")
    private List<String> suggestions;
}
