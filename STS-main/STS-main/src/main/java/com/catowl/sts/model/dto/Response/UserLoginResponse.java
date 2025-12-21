package com.catowl.sts.model.dto.Response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录响应体")
public class UserLoginResponse {
    @Schema(description = "JWT 认证令牌", example = "eyJhbGciOiJIUzI1Ni...")
    private String token;

    @Schema(description = "用户的字符串ID", example = "ulid_user_123")
    private String strId;

    @Schema(description = "用户名", example = "new_user")
    private String username;
}
