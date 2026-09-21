package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 阅读历史（阶段 5 Day 46） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingHistoryVO implements Serializable {

    private Long id;
    private Long contentId;
    private String contentTitle;
    private String contentType;
    /** FREE / SUBSCRIBED */
    private String accessType;
    /** 阅读进度百分比 0-100 */
    private Integer progress;
    private LocalDateTime lastReadTime;
}
