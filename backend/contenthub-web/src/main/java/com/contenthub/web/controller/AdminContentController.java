package com.contenthub.web.controller;

import com.contenthub.common.aspect.ApiOperationLog;
import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.ContentPageReqVO;
import com.contenthub.web.model.req.ReviewReqVO;
import com.contenthub.web.model.vo.ContentDetailVO;
import com.contenthub.web.model.vo.ContentListVO;
import com.contenthub.web.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端内容审核（计划 Day 23-24）。
 *
 * <p>计划表 19 约定的接口：{@code POST /api/admin/contents/{id}/approve}
 * 与 {@code /reject}。{@code /admin/**} 在 Security 中要求 ADMIN 角色。</p>
 */
@RestController
@RequestMapping("/admin/contents")
@Tag(name = "管理端 - 内容审核")
public class AdminContentController {

    private final ContentService contentService;

    public AdminContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    @Operation(summary = "内容列表（可按状态筛选，默认待审核）")
    public Response<PageResponse<ContentListVO>> page(@Validated ContentPageReqVO req) {
        return contentService.pageForReview(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "审核详情（不限状态，返回完整正文供审核判断）")
    public Response<ContentDetailVO> detail(@PathVariable Long id) {
        return contentService.findForReview(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审核通过（PENDING -> PUBLISHED）")
    // 审核动作留痕：@ApiOperationLog 会把入参、出参与耗时打进日志，便于事后追溯是谁放行的
    @ApiOperationLog(description = "审核通过内容")
    public Response<Void> approve(@PathVariable Long id) {
        return contentService.approve(id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审核驳回（PENDING -> REJECTED，需填原因）")
    @ApiOperationLog(description = "审核驳回内容")
    public Response<Void> reject(@PathVariable Long id, @RequestBody @Validated ReviewReqVO req) {
        return contentService.reject(id, req.getReason());
    }
}
