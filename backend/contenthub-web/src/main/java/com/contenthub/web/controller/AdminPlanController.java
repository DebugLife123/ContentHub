package com.contenthub.web.controller;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.SubscriptionPlanVO;
import com.contenthub.web.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端套餐管理（阶段 6 Day 50）。
 *
 * <p>放在 {@code /admin/**} 下而不是 {@code /plans/admin/**}：
 * Security 里 {@code /admin/**} 已经是 ADMIN，新增接口不必再补规则，
 * 也避免出现「/plans」精确匹配与公开 GET 规则互相干扰。</p>
 */
@RestController
@RequestMapping("/admin/plans")
@Tag(name = "管理端 - 套餐")
public class AdminPlanController {

    private final PlanService planService;

    public AdminPlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "全平台套餐（含已下架）")
    public Response<List<SubscriptionPlanVO>> listAll() {
        return planService.listAll();
    }
}
