package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 订阅记录（计划 Day 37：我的订阅页面，分页 + 状态展示） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionVO implements Serializable {

    private Long id;
    private Long planId;
    private String planName;
    private Long creatorId;
    private String creatorName;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /**
     * 展示用状态：ACTIVE 生效中 / EXPIRED 已过期 / CANCELED 已取消。
     *
     * <p>与库里的 status 不同——库里的记录可能仍是 ACTIVE 但 end_time 已过，
     * 这里会按当前时间纠正为 EXPIRED，避免前端显示「生效中」却访问不了内容。</p>
     */
    private String status;

    /** 剩余天数；已过期为 0 */
    private Long remainingDays;

    /** 是否仍然有效 */
    private Boolean valid;

    private LocalDateTime createTime;
}
