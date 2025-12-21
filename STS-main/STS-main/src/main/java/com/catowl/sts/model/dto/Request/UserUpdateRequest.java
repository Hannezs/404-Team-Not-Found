package com.catowl.sts.model.dto.Request;

import com.catowl.sts.model.entity.User;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户更新请求体")
public class UserUpdateRequest {

    @Schema(description = "用户名", example = "new_user")
    private String username;

    @Schema(description = "用户密码", example = "password123")
    private String password;

    @Schema(description = "邮箱", example = "123456@qq.com")
    private String email;

    @ApiModelProperty(
            hidden = true
    )
    public void setUser(User user) {
        user.setUsername(this.username);
        user.setPassword(this.password);
        user.setEmail(this.email);
    }
}
