package com.catowl.sts.model.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable, UserDetails {
    private Long id;
    private String strId;
    private String username;
    private String password; // 字段名已根据 DDL 更新
    private String email;
    private String avatar;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String apiKey;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null; // 这里可以实现角色权限
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled(){
        return true;
    }
}
