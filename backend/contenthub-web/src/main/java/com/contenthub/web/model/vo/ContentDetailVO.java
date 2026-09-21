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
 * <p>阶段 1 只做「已发布内容可读」；按 accessType 决定是否返回正文
 * （免费 / 订阅可见）属于计划阶段 3 Day 25-27 的访问权限判断，此处先原样返回。</p>
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
    private String body;
    private String fileUrl;
    private String accessType;
    private String status;
    private Integer viewCount;
    private Integer likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
