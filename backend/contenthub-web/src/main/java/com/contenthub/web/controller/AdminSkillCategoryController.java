package com.contenthub.web.controller;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.SkillCategoryReqVO;
import com.contenthub.web.model.vo.SkillCategoryVO;
import com.contenthub.web.service.SkillCategoryService;
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

/** 管理端 Skill 分类管理（{@code /admin/**} 要求 ADMIN 角色） */
@RestController
@RequestMapping("/admin/skill-categories")
@Tag(name = "管理端 - Skill 分类")
public class AdminSkillCategoryController {

    private final SkillCategoryService skillCategoryService;

    public AdminSkillCategoryController(SkillCategoryService skillCategoryService) {
        this.skillCategoryService = skillCategoryService;
    }

    @GetMapping
    @Operation(summary = "全部分类（含禁用，管理端用）")
    public Response<List<SkillCategoryVO>> listAll() {
        return skillCategoryService.list(false);
    }

    @PostMapping
    @Operation(summary = "新增分类（同名已删除分类会被复活）")
    public Response<SkillCategoryVO> create(@RequestBody @Validated SkillCategoryReqVO req) {
        return skillCategoryService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改分类")
    public Response<SkillCategoryVO> update(@PathVariable Long id,
                                            @RequestBody @Validated SkillCategoryReqVO req) {
        return skillCategoryService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类（逻辑删除；分类下有 Skill 时拒绝）")
    public Response<Void> delete(@PathVariable Long id) {
        return skillCategoryService.delete(id);
    }
}
