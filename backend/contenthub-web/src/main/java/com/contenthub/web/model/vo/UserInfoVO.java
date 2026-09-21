package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** 当前登录用户信息（计划表 19：GET /api/users/me） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO implements Serializable {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String bio;
    /** USER / CREATOR / ADMIN */
    private String role;
}
