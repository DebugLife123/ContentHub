package com.contenthub.web.controller;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.ReviewReqVO;
import com.contenthub.web.model.vo.CreatorApplicationVO;
import com.contenthub.web.service.CreatorApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 创作者申请审核。
 *
 * <p>与内容审核同一种形态：默认只给待审核队列，通过后把用户角色升为 CREATOR。
 * {@code /admin/**} 在 Security 中要求 ADMIN 角色。</p>
 */
@RestController
@RequestMapping("/admin/creator-applications")
@Tag(name = "管理端 - 创作者申请")
public class AdminCreatorApplicationController {

    private final CreatorApplicationService creatorApplicationService;

    public AdminCreatorApplicationController(CreatorApplicationService creatorApplicationService) {
        this.creatorApplicationService = creatorApplicationService;
    }

    @GetMapping
    @Operation(summary = "申请列表（默认只给待审核）")
    public Response<PageResponse<CreatorApplicationVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status) {
        return creatorApplicationService.pageForReview(pageNum, pageSize, status);
    }

    @GetMapping("/pending-count")
    @Operation(summary = "待审核条数（管理端角标用）")
    public Response<Long> pendingCount() {
        return creatorApplicationService.pendingCount();
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "通过（用户角色升为 CREATOR 并建创作者资料）")
    public Response<Void> approve(@PathVariable Long id) {
        return creatorApplicationService.approve(id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "驳回（需填原因，会通知申请人）")
    public Response<Void> reject(@PathVariable Long id, @RequestBody @Validated ReviewReqVO req) {
        return creatorApplicationService.reject(id, req.getReason());
    }
}
