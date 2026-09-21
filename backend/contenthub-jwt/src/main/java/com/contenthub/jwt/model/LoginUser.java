package com.contenthub.jwt.model;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 登录用户主体。
 *
 * <p>比 Spring 自带的 {@code User} 多带了 {@code userId} 与 {@code role}，
 * 这样在 Service 层做「创作者只能改自己的内容」这类归属判断时，
 * 不必再按用户名回查一次数据库。</p>
 */
@Getter
public class LoginUser implements UserDetails {

    /** 角色前缀，Spring Security 的 hasRole('ADMIN') 实际匹配的是 ROLE_ADMIN */
    public static final String ROLE_PREFIX = "ROLE_";

    private final Long userId;
    private final String username;
    private final String password;
    /** 原始角色名：USER / CREATOR / ADMIN */
    private final String role;

    public LoginUser(Long userId, String username, String password, String role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null || role.isBlank()) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
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
    public boolean isEnabled() {
        return true;
    }
}
