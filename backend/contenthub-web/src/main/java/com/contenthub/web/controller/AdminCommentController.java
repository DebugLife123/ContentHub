package com.contenthub.web.controller;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.CommentVO;
import com.contenthub.web.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 管理端评论管理（阶段 6 Day 54，计划表 19：GET /api/admin/comments） */
@RestController
@RequestMapping("/admin/comments")
@Tag(name = "管理端 - 评论")
public class AdminCommentController {

    private final CommentService commentService;

    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(summary = "评论列表（可按状态筛选，含已隐藏）")
    public Response<PageResponse<CommentVO>> page(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return commentService.adminList(status, pageNum, pageSize);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "隐藏 / 恢复评论")
    public Response<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return commentService.setStatus(id, body.get("status"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论")
    public Response<Void> delete(@PathVariable Long id) {
        return commentService.delete(id);
    }
}
