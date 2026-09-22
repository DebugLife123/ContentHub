package com.contenthub.web.service;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.CreatorRevenueVO;
import com.contenthub.web.model.vo.SubscriptionVO;

public interface SubscriptionService {

    /**
     * 模拟支付并创建/续期订阅（计划 Day 34-35）。
     *
     * <p>没有接真实支付渠道，这里直接生成订阅记录；
     * 以后换成真实支付时，把「支付成功回调」换成调用本方法即可。</p>
     */
    Response<SubscriptionVO> payMock(Long planId, String idempotencyKey);

    /** Compatibility helper for non-HTTP callers; HTTP requests must provide a key. */
    default Response<SubscriptionVO> payMock(Long planId) {
        return payMock(planId, java.util.UUID.randomUUID().toString());
    }

    /** 我的订阅（计划 Day 37） */
    Response<PageResponse<SubscriptionVO>> mySubscriptions(long pageNum, long pageSize);

    /**
     * 提前终止自己的订阅。
     *
     * <p>终止后立即失去访问权限：鉴权条件是 {@code status = ACTIVE 且 end_time > now}，
     * 状态一变就不再有效，不需要等定时任务翻转。</p>
     */
    Response<Void> cancel(Long subscriptionId);

    /** 模拟退款：状态置为 REFUNDED 并记录时间，同样立即失去权限 */
    Response<Void> refund(Long subscriptionId);

    /**
     * 该用户对指定创作者是否持有有效订阅。
     *
     * <p>内容鉴权（计划 Day 36）依赖它。判定条件是
     * {@code status = ACTIVE 且 end_time > now}，因此订阅一过期就立刻失去权限，
     * 不依赖定时任务翻转状态。</p>
     */
    boolean hasActiveSubscription(Long userId, Long creatorId);

    /** 该用户当前是否有任意有效订阅 */
    boolean hasAnyActiveSubscription(Long userId);

    /** 创作者收益统计（只统计自己发布的套餐产生的订阅） */
    Response<CreatorRevenueVO> revenue();
}
