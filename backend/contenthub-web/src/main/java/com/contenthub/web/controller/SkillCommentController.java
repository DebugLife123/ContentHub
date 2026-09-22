package com.contenthub.web.controller;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.CommentReqVO;
import com.contenthub.web.model.vo.SkillCommentVO;
import com.contenthub.web.service.SkillCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Skill 评论（公开读 + 登录写） */
@RestController
@Tag(name = "Skill 评论")
public class SkillCommentController {

    private final SkillCommentService skillCommentService;

    public SkillCommentController(SkillCommentService skillCommentService) {
        this.skillCommentService = skillCommentService;
    }

    @GetMapping("/skills/{id}/comments")
    @Operation(summary = "Skill 下的评论（公开，只返回正常状态）")
    public Response<PageResponse<SkillCommentVO>> list(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "5") long pageSize) {
        return skillCommentService.listBySkill(id, pageNum, pageSize);
    }

    @PostMapping("/skills/{id}/comments")
    @Operation(summary = "发表评论（需登录；Skill 必须已上架）")
    public Response<SkillCommentVO> create(@PathVariable Long id,
                                           @RequestBody @Validated CommentReqVO req) {
        return skillCommentService.create(id, req.getBody());
    }

    @GetMapping("/skill-comments/mine")
    @Operation(summary = "我的 Skill 评论（跨 Skill）")
    public Response<PageResponse<SkillCommentVO>> mine(@RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "10") long pageSize) {
        return skillCommentService.myComments(pageNum, pageSize);
    }

    @DeleteMapping("/skill-comments/{commentId}")
    @Operation(summary = "删除评论（仅本人或管理员）")
    public Response<Void> delete(@PathVariable Long commentId) {
        return skillCommentService.delete(commentId);
    }
}
