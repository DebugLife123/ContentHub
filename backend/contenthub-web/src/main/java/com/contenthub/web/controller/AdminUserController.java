package com.contenthub.web.controller;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.AdminUserVO;
import com.contenthub.web.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 管理端用户管理（阶段 6 Day 51，计划表 19：GET /api/admin/users） */
@RestController
@RequestMapping("/admin/users")
@Tag(name = "管理端 - 用户")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "用户列表（可按关键词与角色筛选）")
    public Response<PageResponse<AdminUserVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return adminUserService.page(keyword, role, pageNum, pageSize);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用 / 禁用账号（禁用后无法登录）")
    public Response<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return adminUserService.updateStatus(id, body.get("status"));
    }
}
