package com.contenthub.web.controller;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.PlanReqVO;
import com.contenthub.web.model.vo.SubscriptionPlanVO;
import com.contenthub.web.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 订阅套餐（计划 Day 30-31，计划表 19：GET /api/plans、POST /api/plans） */
@RestController
@RequestMapping("/plans")
@Tag(name = "订阅套餐")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "已上架套餐（公开）")
    public Response<List<SubscriptionPlanVO>> listActive() {
        return planService.listActive();
    }

    @GetMapping("/mine")
    @Operation(summary = "我的套餐（含已下架，需创作者或管理员）")
    public Response<List<SubscriptionPlanVO>> listMine() {
        return planService.listMine();
    }

    @PostMapping
    @Operation(summary = "新增套餐（需创作者或管理员）")
    public Response<SubscriptionPlanVO> create(@RequestBody @Validated PlanReqVO req) {
        return planService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改套餐（仅所属创作者或管理员）")
    public Response<SubscriptionPlanVO> update(@PathVariable Long id, @RequestBody @Validated PlanReqVO req) {
        return planService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除套餐（逻辑删除；已有订阅记录时拒绝）")
    public Response<Void> delete(@PathVariable Long id) {
        return planService.delete(id);
    }
}
