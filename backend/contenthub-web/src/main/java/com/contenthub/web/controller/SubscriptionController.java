package com.contenthub.web.controller;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.SubscriptionVO;
import com.contenthub.web.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订阅（计划 Day 34-37，计划表 19）。
 *
 * <p>「模拟支付」刻意单独成一个接口：以后接真实支付渠道时，
 * 把这里换成「下单 -> 支付渠道跳转 -> 回调里创建订阅」即可，
 * 订阅本身的创建逻辑不用改。</p>
 */
@RestController
@RequestMapping("/subscriptions")
@Tag(name = "订阅")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/{planId}/pay/mock")
    @Operation(summary = "模拟支付并创建/续期订阅")
    public Response<SubscriptionVO> payMock(@PathVariable Long planId) {
        return subscriptionService.payMock(planId);
    }

    @GetMapping("/my")
    @Operation(summary = "我的订阅")
    public Response<PageResponse<SubscriptionVO>> my(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return subscriptionService.mySubscriptions(pageNum, pageSize);
    }
}
