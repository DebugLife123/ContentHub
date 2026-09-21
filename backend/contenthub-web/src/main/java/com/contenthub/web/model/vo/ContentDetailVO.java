package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容详情。
 *
 * <p>阶段 3 Day 26：「预览与完整正文分开 —— 无权限时只返回预览信息」。
 * 因此 {@code body} 在无权限时为 null，改为返回 {@code bodyPreview}，
 * 并用 {@code locked} 明确告知前端「这是被锁住的内容」而不是「内容为空」。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentDetailVO implements Serializable {

    private Long id;
    private Long creatorId;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String summary;
    private String cover;
    private String contentType;

    /** 完整正文；无访问权限时为 null */
    private String body;

    /** 无权限时返回的试读片段 */
    private String bodyPreview;

    /** 是否被访问权限锁住（true 时 body 为 null） */
    private Boolean locked;

    /** 被锁住的原因，用于前端展示订阅引导 */
    private String lockReason;

    /** 完整附件地址；无权限时为 null */
    private String fileUrl;

    /** FREE / SUBSCRIBED */
    private String accessType;
    private String status;

    /** 审核驳回原因，仅 status=REJECTED 时有值 */
    private String rejectReason;

    private Integer viewCount;
    private Integer likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 当前登录用户是否已收藏（未登录时恒为 false） */
    private Boolean favorited;

    /** 收藏数 */
    private Long favoriteCount;
}
