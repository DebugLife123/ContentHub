package com.contenthub.jwt.handler;

import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.utils.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 已登录、但角色不够时的处理（计划 Day 19 的权限验收依赖它）。
 *
 * <p>改造前这里只打日志、不写响应体，前端会收到一个空 body 的 403，
 * 无法区分「没登录」和「登录了但权限不够」。现在统一返回 403 + 业务错误码。</p>
 */
@Slf4j
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("已登录但权限不足：{} {}", request.getMethod(), request.getRequestURI());
        ResultUtil.fail(response, HttpStatus.FORBIDDEN.value(), Response.fail(ResponseCodeEnum.FORBIDDEN));
    }
}
