package com.contenthub.web.controller;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.CreatorProfileReqVO;
import com.contenthub.web.model.vo.CreatorProfileVO;
import com.contenthub.web.service.CreatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 创作者中心（计划 Day 20）。
 *
 * <p>{@code /creator/**} 在 Security 中要求 CREATOR 或 ADMIN 角色，
 * 唯独 {@code /creators/**}（公开资料）是放行的——注意单复数不同是有意的：
 * 前者是"我的创作者后台"，后者是"给别人看的创作者主页"。</p>
 */
@RestController
@Tag(name = "创作者")
public class CreatorController {

    private final CreatorService creatorService;

    public CreatorController(CreatorService creatorService) {
        this.creatorService = creatorService;
    }

    @PostMapping("/creator/apply")
    @Operation(summary = "申请成为创作者（普通用户 -> 创作者）")
    public Response<CreatorProfileVO> apply() {
        return creatorService.apply();
    }

    @GetMapping("/creator/profile")
    @Operation(summary = "我的创作者资料")
    public Response<CreatorProfileVO> myProfile() {
        return creatorService.myProfile();
    }

    @PutMapping("/creator/profile")
    @Operation(summary = "修改创作者资料")
    public Response<CreatorProfileVO> updateProfile(@RequestBody @Validated CreatorProfileReqVO req) {
        return creatorService.updateProfile(req);
    }

    @GetMapping("/creators/{userId}")
    @Operation(summary = "创作者公开资料")
    public Response<CreatorProfileVO> publicProfile(@PathVariable Long userId) {
        return creatorService.publicProfile(userId);
    }
}
