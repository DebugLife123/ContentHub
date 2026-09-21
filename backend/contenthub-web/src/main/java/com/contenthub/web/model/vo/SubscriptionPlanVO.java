package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订阅套餐（计划 Day 30-31） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanVO implements Serializable {

    private Long id;
    private Long creatorId;
    private String creatorName;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationDays;
    /** ACTIVE 上架 / INACTIVE 下架 */
    private String status;
    /** 该套餐的订阅人数 */
    private Long subscriberCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
