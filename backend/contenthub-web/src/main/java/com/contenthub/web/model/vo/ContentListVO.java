package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容列表项。
 *
 * <p>刻意不包含 {@code body} / {@code fileUrl} / {@code isDeleted} 等字段：
 * 改造前接口直接返回 ContentDO，把逻辑删除标记、正文全文都暴露给了列表页。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentListVO implements Serializable {

    private Long id;
    private Long creatorId;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String summary;
    private String cover;
    /** ARTICLE/TUTORIAL/EBOOK/VIDEO/PDF/CODE/PROMPT/DATASET/COLUMN */
    private String contentType;
    /** FREE / SUBSCRIBED */
    private String accessType;
    /** DRAFT / PENDING / PUBLISHED / REJECTED / OFFLINE */
    private String status;
    /** 审核驳回原因，仅 status=REJECTED 时有值 */
    private String rejectReason;
    private Integer viewCount;
    private Integer likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
