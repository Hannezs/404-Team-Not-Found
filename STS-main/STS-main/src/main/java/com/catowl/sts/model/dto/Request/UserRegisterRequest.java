package com.catowl.sts.model.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户注册请求体")
public class UserRegisterRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    @Schema(description = "用户名", example = "new_user", required = true)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    @Schema(description = "用户密码", example = "password123", required = true)
    private String password;

}
