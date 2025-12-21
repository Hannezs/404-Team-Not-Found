package com.catowl.sts.model.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录请求体")
public class UserLoginRequest {
    @NotBlank
    @Schema(description = "用户名", example = "new_user", required = true)
    private String username;

    @NotBlank
    @Schema(description = "用户密码", example = "password123", required = true)
    private String password;
}
