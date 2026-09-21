package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 评论（阶段 5 Day 45 / Day 54 管理端评论管理） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO implements Serializable {

    private Long id;
    private Long contentId;
    /** 评论所在内容标题，个人中心「我的评论」里展示 */
    private String contentTitle;
    private Long userId;
    private String username;
    /** NORMAL 正常 / HIDDEN 已隐藏 */
    private String status;
    private String body;
    /** 当前登录用户是否有权删除（本人或管理员） */
    private Boolean canDelete;
    private LocalDateTime createTime;
}
