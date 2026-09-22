package com.contenthub.web.controller;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.ChangePasswordReqVO;
import com.contenthub.web.model.req.UserProfileReqVO;
import com.contenthub.web.model.vo.CommentVO;
import com.contenthub.web.model.vo.ContentListVO;
import com.contenthub.web.model.vo.ReadingHistoryVO;
import com.contenthub.web.model.vo.UserInfoVO;
import com.contenthub.web.service.CommentService;
import com.contenthub.web.service.FavoriteService;
import com.contenthub.web.service.ReadingHistoryService;
import com.contenthub.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "用户")
public class UserController {

    private final UserService userService;
    private final FavoriteService favoriteService;
    private final ReadingHistoryService readingHistoryService;
    private final CommentService commentService;

    public UserController(UserService userService,
                          FavoriteService favoriteService,
                          ReadingHistoryService readingHistoryService,
                          CommentService commentService) {
        this.userService = userService;
        this.favoriteService = favoriteService;
        this.readingHistoryService = readingHistoryService;
        this.commentService = commentService;
    }

    @GetMapping("/me")
    @Operation(summary = "当前登录用户信息")
    public Response<UserInfoVO> me() {
        return userService.currentUser();
    }

    @PutMapping("/me")
    @Operation(summary = "修改个人资料（昵称 / 头像 / 邮箱 / 简介）")
    public Response<UserInfoVO> updateProfile(@RequestBody @Validated UserProfileReqVO req) {
        return userService.updateProfile(req);
    }

    @PutMapping("/me/password")
    @Operation(summary = "修改密码（需校验原密码）")
    public Response<Void> changePassword(@RequestBody @Validated ChangePasswordReqVO req) {
        return userService.changePassword(req);
    }

    @GetMapping("/me/favorites")
    @Operation(summary = "我的收藏")
    public Response<PageResponse<ContentListVO>> myFavorites(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "9") long pageSize) {
        return favoriteService.myFavorites(pageNum, pageSize);
    }

    @GetMapping("/me/history")
    @Operation(summary = "我的阅读历史（阶段 5 Day 46）")
    public Response<PageResponse<ReadingHistoryVO>> myHistory(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return readingHistoryService.myHistory(pageNum, pageSize);
    }

    @GetMapping("/me/comments")
    @Operation(summary = "我的评论（阶段 5 Day 45）")
    public Response<PageResponse<CommentVO>> myComments(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return commentService.myComments(pageNum, pageSize);
    }
}
