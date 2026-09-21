package com.contenthub.jwt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRspVO {

    /** Token 值 */
    private String token;

    /** Token 类型，方便前端拼接 Authorization 头 */
    @Builder.Default
    private String tokenType = "Bearer";

    /** 有效期（分钟） */
    private Long expiresInMinutes;

    /** 当前登录用户的角色：USER / CREATOR / ADMIN */
    private String role;

    /** 用户名 */
    private String username;
}
