package com.contenthub.common.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订阅套餐（计划 Day 30：价格、周期、状态） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("subscription_plans")
public class SubscriptionPlanDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套餐归属创作者 */
    private Long creatorId;

    private String name;

    private String description;

    private BigDecimal price;

    /** 有效天数，用于计算订阅到期时间 */
    private Integer durationDays;

    /** ACTIVE 上架 / INACTIVE 下架 */
    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
