package com.contenthub.web.controller;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.RegisterReqVO;
import com.contenthub.web.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（计划表 19）。
 *
 * <p>注意：{@code POST /auth/login} 不在这里——它由
 * {@code JwtAuthenticationFilter} 直接拦截处理，因此本类中看不到登录方法。</p>
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "认证")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "注册")
    public Response<Void> register(@RequestBody @Validated RegisterReqVO req) {
        return authService.register(req);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录（使当前 token 立即失效）")
    public Response<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.logout(authorization);
    }
}
