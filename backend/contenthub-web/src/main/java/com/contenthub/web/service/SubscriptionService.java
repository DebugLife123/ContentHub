package com.contenthub.web.service;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.SubscriptionVO;

public interface SubscriptionService {

    /**
     * 模拟支付并创建/续期订阅（计划 Day 34-35）。
     *
     * <p>没有接真实支付渠道，这里直接生成订阅记录；
     * 以后换成真实支付时，把「支付成功回调」换成调用本方法即可。</p>
     */
    Response<SubscriptionVO> payMock(Long planId);

    /** 我的订阅（计划 Day 37） */
    Response<PageResponse<SubscriptionVO>> mySubscriptions(long pageNum, long pageSize);

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
}
