package com.contenthub.web.controller;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.SkillCategoryVO;
import com.contenthub.web.service.SkillCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Skill 分类（公开读）。
 *
 * <p>写接口在 {@code AdminSkillCategoryController}，规则见 {@code WebSecurityConfig}。</p>
 */
@RestController
@RequestMapping("/skill-categories")
@Tag(name = "Skill 分类")
public class SkillCategoryController {

    private final SkillCategoryService skillCategoryService;

    public SkillCategoryController(SkillCategoryService skillCategoryService) {
        this.skillCategoryService = skillCategoryService;
    }

    @GetMapping
    @Operation(summary = "Skill 分类列表（仅启用中，公开）")
    public Response<List<SkillCategoryVO>> list() {
        return skillCategoryService.list(true);
    }
}
