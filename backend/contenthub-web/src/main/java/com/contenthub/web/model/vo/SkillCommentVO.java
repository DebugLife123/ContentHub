package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Skill 评论 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillCommentVO implements Serializable {

    private Long id;
    private Long skillId;
    /** Skill 名称，管理端列表与「我的评论」展示用 */
    private String skillName;
    private Long userId;
    private String username;
    /** NORMAL / HIDDEN */
    private String status;
    private String body;
    /** 当前登录用户能否删这条（作者本人或管理员） */
    private Boolean canDelete;
    private LocalDateTime createTime;
}
