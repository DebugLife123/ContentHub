package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 创作者资料（计划 Day 20） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorProfileVO implements Serializable {

    private Long id;
    private Long userId;
    private String displayName;
    private String intro;
    private Boolean verified;
    private Integer subscriberCount;
    private Integer contentCount;

    /** 已发布内容数（实时统计） */
    private Long publishedCount;

    private LocalDateTime createTime;
}
