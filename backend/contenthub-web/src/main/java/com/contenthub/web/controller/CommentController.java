package com.contenthub.web.controller;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.CommentReqVO;
import com.contenthub.web.model.req.ProgressReqVO;
import com.contenthub.web.model.vo.CommentVO;
import com.contenthub.web.service.CommentService;
import com.contenthub.web.service.ReadingHistoryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论与阅读进度（阶段 5 Day 45-46，计划表 19）。
 *
 * <p>评论挂在内容下（{@code /contents/{id}/comments}），删除走独立的
 * {@code /comments/{id}}——因为删除时需要的是评论ID而不是内容ID。</p>
 */
@RestController
@Tag(name = "评论与阅读")
public class CommentController {

    private final CommentService commentService;
    private final ReadingHistoryService readingHistoryService;

    public CommentController(CommentService commentService, ReadingHistoryService readingHistoryService) {
        this.commentService = commentService;
        this.readingHistoryService = readingHistoryService;
    }

    @GetMapping("/contents/{id}/comments")
    @Operation(summary = "内容下的评论（公开，只返回正常状态）")
    public Response<PageResponse<CommentVO>> list(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return commentService.listByContent(id, pageNum, pageSize);
    }

    @PostMapping("/contents/{id}/comments")
    @Operation(summary = "发表评论（需登录）")
    public Response<CommentVO> create(@PathVariable Long id, @RequestBody @Validated CommentReqVO req) {
        return commentService.create(id, req);
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "删除评论（仅本人或管理员）")
    public Response<Void> delete(@PathVariable Long commentId) {
        return commentService.delete(commentId);
    }

    @PutMapping("/contents/{id}/progress")
    @Operation(summary = "更新阅读进度（需登录）")
    public Response<Void> updateProgress(@PathVariable Long id, @RequestBody @Validated ProgressReqVO req) {
        return readingHistoryService.updateProgress(id, req.getProgress());
    }
}
