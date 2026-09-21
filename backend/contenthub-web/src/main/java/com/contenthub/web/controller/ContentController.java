package com.contenthub.web.controller;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.ContentPageReqVO;
import com.contenthub.web.model.req.ContentReqVO;
import com.contenthub.web.model.vo.ContentDetailVO;
import com.contenthub.web.model.vo.ContentListVO;
import com.contenthub.web.service.ContentService;
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
 * 内容接口（计划表 19，阶段 1 Day 8-11）。
 *
 * <p>路径说明：Spring MVC 会优先匹配字面量路径，因此 {@code /contents/mine}
 * 不会被 {@code /contents/{id}} 当成 id="mine" 解析。</p>
 */
@RestController
@RequestMapping("/contents")
@Tag(name = "内容")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/page")
    @Operation(summary = "内容分页查询（仅已发布，公开）")
    public Response<PageResponse<ContentListVO>> page(@Validated ContentPageReqVO req) {
        return contentService.pagePublished(req);
    }

    @GetMapping("/mine")
    @Operation(summary = "我的内容（含草稿/下架，需创作者或管理员）")
    public Response<PageResponse<ContentListVO>> mine(@Validated ContentPageReqVO req) {
        return contentService.pageMine(req);
    }

    @GetMapping("/mine/{id}")
    @Operation(summary = "我的内容详情（不限状态，供编辑回显，需创作者或管理员）")
    public Response<ContentDetailVO> mineDetail(@PathVariable Long id) {
        return contentService.findMineById(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "内容详情（仅已发布，公开）")
    public Response<ContentDetailVO> detail(@PathVariable Long id) {
        return contentService.findPublishedById(id);
    }

    @PostMapping
    @Operation(summary = "新增内容（需创作者或管理员）")
    public Response<Long> create(@RequestBody @Validated ContentReqVO req) {
        return contentService.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑内容（仅作者本人或管理员）")
    public Response<Void> update(@PathVariable Long id, @RequestBody @Validated ContentReqVO req) {
        return contentService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除内容（逻辑删除，仅作者本人或管理员）")
    public Response<Void> delete(@PathVariable Long id) {
        return contentService.delete(id);
    }
}
