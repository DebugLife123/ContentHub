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

import java.time.LocalDateTime;

/**
 * 用户订阅记录（计划 Day 32：start/end/status）。
 *
 * <p>是否「仍然有效」由 {@code status = ACTIVE 且 end_time > now} 共同决定；
 * 到期不依赖定时任务翻转状态，查询时按时间判断即可，
 * 因此服务重启或定时任务漏跑都不会把已过期订阅误判为有效。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("subscriptions")
public class SubscriptionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long planId;

    /** 冗余一份创作者ID，内容鉴权时可直接按创作者判断，少一次 join */
    private Long creatorId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** ACTIVE 生效中 / EXPIRED 已过期 / CANCELED 已取消 */
    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
