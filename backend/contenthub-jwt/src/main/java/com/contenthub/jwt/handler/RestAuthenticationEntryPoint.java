package com.contenthub.jwt.handler;

import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.utils.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 未登录访问受保护资源时的处理，统一返回 401。
 *
 * <p>注意：改造前这里在 InsufficientAuthenticationException 分支写出响应后又继续执行了一次
 * {@code ResultUtil.fail}，而该方法会 close writer——第二次写入作用在已提交的响应上。
 * 现在改为 if/else，只写一次。</p>
 */
@Slf4j
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.warn("未登录或登录已失效，访问受保护资源：{} {}", request.getMethod(), request.getRequestURI());

        if (authException instanceof InsufficientAuthenticationException) {
            ResultUtil.fail(response, HttpStatus.UNAUTHORIZED.value(), Response.fail(ResponseCodeEnum.UNAUTHORIZED));
            return;
        }

        // Token 不可用 / 已失效 / 已在别处登出
        String message = authException.getMessage() == null
                ? ResponseCodeEnum.UNAUTHORIZED.getErrorMessage()
                : authException.getMessage();
        ResultUtil.fail(response, HttpStatus.UNAUTHORIZED.value(),
                Response.fail(ResponseCodeEnum.UNAUTHORIZED.getErrorCode(), message));
    }
}
