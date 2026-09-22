package com.contenthub.web.controller;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.CreatorApplyReqVO;
import com.contenthub.web.model.req.CreatorProfileReqVO;
import com.contenthub.web.model.vo.CreatorApplicationVO;
import com.contenthub.web.model.vo.CreatorDashboardVO;
import com.contenthub.web.model.vo.CreatorProfileVO;
import com.contenthub.web.model.vo.CreatorRevenueVO;
import com.contenthub.web.service.CreatorApplicationService;
import com.contenthub.web.service.CreatorService;
import com.contenthub.web.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 创作者中心（计划 Day 20）。
 *
 * <p>{@code /creator/**} 在 Security 中要求 CREATOR 或 ADMIN 角色，
 * 唯独 {@code /creators/**}（公开资料）是放行的——注意单复数不同是有意的：
 * 前者是"我的创作者后台"，后者是"给别人看的创作者主页"。</p>
 */
@RestController
@Tag(name = "创作者")
public class CreatorController {

    private final CreatorService creatorService;
    private final CreatorApplicationService creatorApplicationService;
    private final SubscriptionService subscriptionService;

    public CreatorController(CreatorService creatorService,
                             CreatorApplicationService creatorApplicationService,
                             SubscriptionService subscriptionService) {
        this.creatorService = creatorService;
        this.creatorApplicationService = creatorApplicationService;
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/creator/apply")
    @Operation(summary = "提交创作者申请（进入待审核，管理员通过后才升级角色）")
    public Response<CreatorApplicationVO> apply(@RequestBody(required = false) @Validated CreatorApplyReqVO req) {
        return creatorApplicationService.apply(req);
    }

    @GetMapping("/creator/application")
    @Operation(summary = "我的最近一条创作者申请（没申请过返回 null）")
    public Response<CreatorApplicationVO> myApplication() {
        return creatorApplicationService.myApplication();
    }

    @GetMapping("/creator/profile")
    @Operation(summary = "我的创作者资料")
    public Response<CreatorProfileVO> myProfile() {
        return creatorService.myProfile();
    }

    @PutMapping("/creator/profile")
    @Operation(summary = "修改创作者资料")
    public Response<CreatorProfileVO> updateProfile(@RequestBody @Validated CreatorProfileReqVO req) {
        return creatorService.updateProfile(req);
    }

    @GetMapping("/creator/dashboard")
    @Operation(summary = "创作者仪表盘统计（内容数 / 阅读量 / 收藏量 / 订阅人数）")
    public Response<CreatorDashboardVO> dashboard() {
        return creatorService.dashboard();
    }

    @GetMapping("/creator/revenue")
    @Operation(summary = "创作者收益（累计 / 按套餐 / 按月 / 最近订单）")
    public Response<CreatorRevenueVO> revenue() {
        return subscriptionService.revenue();
    }

    @GetMapping("/creators/{userId}")
    @Operation(summary = "创作者公开资料")
    public Response<CreatorProfileVO> publicProfile(@PathVariable Long userId) {
        return creatorService.publicProfile(userId);
    }
}
