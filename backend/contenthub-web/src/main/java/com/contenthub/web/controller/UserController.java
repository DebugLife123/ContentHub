package com.contenthub.web.controller;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.ContentListVO;
import com.contenthub.web.model.vo.UserInfoVO;
import com.contenthub.web.service.FavoriteService;
import com.contenthub.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "用户")
public class UserController {

    private final UserService userService;
    private final FavoriteService favoriteService;

    public UserController(UserService userService, FavoriteService favoriteService) {
        this.userService = userService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/me")
    @Operation(summary = "当前登录用户信息")
    public Response<UserInfoVO> me() {
        return userService.currentUser();
    }

    @GetMapping("/me/favorites")
    @Operation(summary = "我的收藏")
    public Response<PageResponse<ContentListVO>> myFavorites(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "9") long pageSize) {
        return favoriteService.myFavorites(pageNum, pageSize);
    }
}
