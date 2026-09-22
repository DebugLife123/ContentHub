package com.contenthub.web.controller;

import com.contenthub.common.aspect.ApiOperationLog;
import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.SkillPageReqVO;
import com.contenthub.web.model.req.SkillReqVO;
import com.contenthub.web.model.vo.SkillDetailVO;
import com.contenthub.web.model.vo.SkillListVO;
import com.contenthub.web.service.SkillService;
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

/**
 * 管理端 Skill 商城管理。
 *
 * <p>Skill 由管理员自己维护，没有创作者投稿与审核环节：这里直接是增删改查
 * 加上架/下架，不像内容那样还要先提交再审批。</p>
 *
 * <p>{@code /admin/**} 在 Security 中要求 ADMIN 角色。</p>
 */
@RestController
@RequestMapping("/admin/skills")
@Tag(name = "管理端 - Skill 商城")
public class AdminSkillController {

    private final SkillService skillService;

    public AdminSkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    @Operation(summary = "Skill 列表（含草稿与已下架，可按状态筛选）")
    public Response<PageResponse<SkillListVO>> page(@Validated SkillPageReqVO req) {
        return skillService.pageForAdmin(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Skill 详情（不限状态，管理端回显用）")
    public Response<SkillDetailVO> detail(@PathVariable Long id) {
        return skillService.findByIdForAdmin(id);
    }

    @PostMapping
    @Operation(summary = "新增 Skill（落库为草稿）")
    public Response<Long> create(@RequestBody @Validated SkillReqVO req) {
        return skillService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑 Skill（不改动上架状态）")
    public Response<Void> update(@PathVariable Long id, @RequestBody @Validated SkillReqVO req) {
        return skillService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除 Skill（逻辑删除）")
    public Response<Void> delete(@PathVariable Long id) {
        return skillService.delete(id);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "上架（草稿/已下架 -> 已上架）")
    @ApiOperationLog(description = "上架 Skill")
    public Response<Void> publish(@PathVariable Long id) {
        return skillService.publish(id);
    }

    @PostMapping("/{id}/offline")
    @Operation(summary = "下架（已上架 -> 已下架）")
    @ApiOperationLog(description = "下架 Skill")
    public Response<Void> offline(@PathVariable Long id) {
        return skillService.offline(id);
    }
}
