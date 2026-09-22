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

/**
 * 订阅支付流水（模拟支付的不可变账本）。
 *
 * <p>每一次「支付成功」落一行，存下单当时的金额与套餐名快照。
 * 收益统计读这张表而不是拿「当前套餐价 × 订阅数」推算，
 * 否则创作者一改价，历史收益会跟着被改写。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("subscription_payments")
public class SubscriptionPaymentDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long subscriptionId;

    private Long planId;

    private Long creatorId;

    /** 幂等键：同一用户下唯一，重复提交同一个 key 只生效一次 */
    private String idempotencyKey;

    /** 成交金额快照 */
    private BigDecimal amountSnapshot;

    /** 成交时套餐名快照 */
    private String planNameSnapshot;

    /** 成交时套餐天数快照 */
    private Integer durationDaysSnapshot;

    private LocalDateTime paidAt;

    /** 1 = 历史数据回填的估算值（金额取自当时的套餐现价，不是真实成交价） */
    private Boolean estimated;

    /**
     * 逻辑删除。
     *
     * <p>必须显式写 {@code @TableField("is_deleted")}：MyBatis-Plus 的驼峰转下划线
     * 只会把 {@code deleted} 映射成 {@code deleted}，而不会加上 {@code is_} 前缀。
     * 漏了这个注解，所有查询都会变成 {@code WHERE deleted=0}，
     * 运行时报 {@code Unknown column 'deleted'}。其他 DO 也都是显式声明的。</p>
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
