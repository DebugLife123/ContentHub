package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 管理端用户列表项（阶段 6 Day 51） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserVO implements Serializable {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    /** USER / CREATOR / ADMIN */
    private String role;
    /** ENABLED / DISABLED */
    private String status;
    private LocalDateTime createTime;

    /** 该用户发布的内容数，便于判断是否可安全禁用 */
    private Long contentCount;
}
