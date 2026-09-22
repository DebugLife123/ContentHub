package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 创作者收益。
 *
 * <p>口径：收益 = 已支付且**未退款**的订阅金额。
 * 用户提前终止（CANCELED）仍然计入——那段时间的钱创作者已经赚到了；
 * 只有退款（REFUNDED）才从收益里扣掉。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorRevenueVO implements Serializable {

    /** 累计收益（已扣退款） */
    private BigDecimal totalRevenue;
    /** 已退款金额，单独展示，避免让人以为收益是毛收入 */
    private BigDecimal refundedAmount;
    /** 订单总数（含已退款与已终止） */
    private long subscriptionCount;
    /** 当前生效中的订阅数 */
    private long activeCount;
    /** Number of legacy rows whose amount was estimated during migration. */
    private long estimatedPaymentCount;
    private String estimateNote;

    private List<PlanRevenue> byPlan;
    private List<MonthlyRevenue> monthly;
    /** 最近的订阅记录 */
    private List<SubscriptionVO> recent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanRevenue implements Serializable {
        private Long planId;
        private String planName;
        private BigDecimal price;
        private long count;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue implements Serializable {
        /** yyyy-MM */
        private String month;
        private long count;
        private BigDecimal amount;
    }
}
