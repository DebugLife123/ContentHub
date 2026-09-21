package com.contenthub.web.controller;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.UserInfoVO;
import com.contenthub.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "用户")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "当前登录用户信息")
    public Response<UserInfoVO> me() {
        return userService.currentUser();
    }
}
