package com.contenthub.jwt.handler;

import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginRspVO;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.jwt.service.LoginTokenService;
import com.contenthub.jwt.utils.JwtTokenHelper;
import com.contenthub.jwt.utils.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class RestAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private JwtTokenHelper jwtTokenHelper;

    @Autowired
    private LoginTokenService loginTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String username = loginUser.getUsername();

        // 1. 签发 JWT
        String token = jwtTokenHelper.generateToken(username);

        // 2. 写入 Redis 并设置过期时间（计划 Day 16）。
        //    这样退出登录才能真正让 token 失效——纯 JWT 是签发即不可撤回的。
        loginTokenService.save(token, username);
        log.info("用户 {} 登录成功，token 已写入 Redis，TTL={} 分钟",
                username, loginTokenService.getTokenExpireMinutes());

        LoginRspVO loginRspVO = LoginRspVO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMinutes(loginTokenService.getTokenExpireMinutes())
                .role(loginUser.getRole())
                .username(username)
                .build();

        ResultUtil.ok(response, Response.success(loginRspVO));
    }
}
