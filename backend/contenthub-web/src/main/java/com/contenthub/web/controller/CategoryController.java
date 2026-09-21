package com.contenthub.web.controller;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.CategoryReqVO;
import com.contenthub.web.model.vo.CategoryVO;
import com.contenthub.web.service.CategoryService;
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

/**
 * 内容分类（计划表 19，阶段 1 Day 7）。
 *
 * <p>读接口公开，写接口仅管理员——具体规则见 {@code WebSecurityConfig}。</p>
 */
@RestController
@RequestMapping("/categories")
@Tag(name = "内容分类")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "分类列表（仅启用中，公开）")
    public Response<List<CategoryVO>> list() {
        return categoryService.list(true);
    }

    @GetMapping("/all")
    @Operation(summary = "全部分类（含禁用，仅管理员，供后台管理页使用）")
    public Response<List<CategoryVO>> listAll() {
        return categoryService.list(false);
    }

    @PostMapping
    @Operation(summary = "新增分类（仅管理员）")
    public Response<CategoryVO> create(@RequestBody @Validated CategoryReqVO req) {
        return categoryService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改分类（仅管理员）")
    public Response<CategoryVO> update(@PathVariable Long id, @RequestBody @Validated CategoryReqVO req) {
        return categoryService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类（仅管理员，逻辑删除）")
    public Response<Void> delete(@PathVariable Long id) {
        return categoryService.delete(id);
    }
}
