package com.contenthub.web.controller;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.ContentPageReqVO;
import com.contenthub.web.model.req.ContentReqVO;
import com.contenthub.web.model.vo.ContentDetailVO;
import com.contenthub.web.model.vo.ContentListVO;
import com.contenthub.web.service.ContentService;
import com.contenthub.web.service.FavoriteService;
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
 * 内容接口（计划表 19，阶段 1 Day 8-11 + 阶段 3 Day 21-28）。
 *
 * <p>路径说明：Spring MVC 会优先匹配字面量路径，因此 {@code /contents/mine}
 * 不会被 {@code /contents/{id}} 当成 id="mine" 解析。</p>
 */
@RestController
@RequestMapping("/contents")
@Tag(name = "内容")
public class ContentController {

    private final ContentService contentService;
    private final FavoriteService favoriteService;

    public ContentController(ContentService contentService, FavoriteService favoriteService) {
        this.contentService = contentService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/page")
    @Operation(summary = "内容分页查询（仅已发布，公开）")
    public Response<PageResponse<ContentListVO>> page(@Validated ContentPageReqVO req) {
        return contentService.pagePublished(req);
    }

    @GetMapping("/mine")
    @Operation(summary = "我的内容（含全部状态，需创作者或管理员）")
    public Response<PageResponse<ContentListVO>> mine(@Validated ContentPageReqVO req) {
        return contentService.pageMine(req);
    }

    @GetMapping("/mine/{id}")
    @Operation(summary = "我的内容详情（不限状态，编辑回显用）")
    public Response<ContentDetailVO> mineDetail(@PathVariable Long id) {
        return contentService.findMineById(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "内容详情（仅已发布；无订阅权限时只返回试读片段）")
    public Response<ContentDetailVO> detail(@PathVariable Long id) {
        return contentService.findPublishedById(id);
    }

    @PostMapping
    @Operation(summary = "新增内容（落库为草稿，需创作者或管理员）")
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

    // ---------------------------------------------------------------- 状态流转

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审核（草稿/已驳回/已下架 -> 待审核）")
    public Response<Void> submit(@PathVariable Long id) {
        return contentService.submit(id);
    }

    @PostMapping("/{id}/offline")
    @Operation(summary = "下架（已发布 -> 已下架）")
    public Response<Void> offline(@PathVariable Long id) {
        return contentService.offline(id);
    }

    // ------------------------------------------------------------------ 收藏

    @PostMapping("/{id}/favorite")
    @Operation(summary = "收藏（需登录，重复收藏会被拒绝）")
    public Response<Void> favorite(@PathVariable Long id) {
        return favoriteService.favorite(id);
    }

    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "取消收藏（需登录）")
    public Response<Void> unfavorite(@PathVariable Long id) {
        return favoriteService.unfavorite(id);
    }
}
