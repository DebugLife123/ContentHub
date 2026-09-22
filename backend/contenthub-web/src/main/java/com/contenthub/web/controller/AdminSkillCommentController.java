package com.contenthub.web.controller;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.SkillCommentVO;
import com.contenthub.web.service.SkillCommentService;
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

/** 管理端 - Skill 评论管理（{@code /admin/**} 要求 ADMIN 角色） */
@RestController
@RequestMapping("/admin/skill-comments")
@Tag(name = "管理端 - Skill 评论")
public class AdminSkillCommentController {

    private final SkillCommentService skillCommentService;

    public AdminSkillCommentController(SkillCommentService skillCommentService) {
        this.skillCommentService = skillCommentService;
    }

    @GetMapping
    @Operation(summary = "评论列表（含已隐藏，可按状态筛选）")
    public Response<PageResponse<SkillCommentVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "10") long pageSize,
                                                       @RequestParam(required = false) String status) {
        return skillCommentService.pageForAdmin(pageNum, pageSize, status);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "隐藏 / 恢复评论")
    public Response<Void> updateStatus(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        return skillCommentService.updateStatus(id, body.get("status"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论")
    public Response<Void> delete(@PathVariable Long id) {
        return skillCommentService.delete(id);
    }
}
