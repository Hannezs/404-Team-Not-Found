package com.catowl.sts.model.dto.Response;

import com.catowl.sts.model.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "获取当前用户信息的请求体")
public class UserResponse {
    @Schema(description = "用户的字符串ID", example = "ulid_user_123")
    private String strId;

    @Schema(description = "用户名", example = "new_user")
    private String username;

    @Schema(description = "用户邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "用户头像 URL", example = "https://example.com/avatar.png")
    private String avatar;

    @Schema(description = "Dify API Key", example = "key-...")
    private String apiKey;

    @Schema(description = "账号创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "账号更新时间")
    private LocalDateTime updatedAt;

    @Schema(hidden = true)
    public void setUser(User user){
        this.strId=user.getStrId();
        this.username=user.getUsername();
        this.email=user.getEmail();
        this.avatar=user.getAvatar();
        this.apiKey=user.getApiKey();
        this.createdAt=user.getCreatedAt();
        this.updatedAt=user.getUpdatedAt();
    }
}
