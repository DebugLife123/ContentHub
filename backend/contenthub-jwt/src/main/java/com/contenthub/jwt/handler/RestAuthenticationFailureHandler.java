package com.contenthub.jwt.handler;

import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.exception.UsernameOrPasswordNullException;
import com.contenthub.jwt.utils.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录失败处理。
 *
 * <p>注意：改造前这里分支里写出响应后没有 return，会继续执行最后一行再写一次，
 * 而 {@code ResultUtil.fail} 内部会 close writer——第二次写入作用在已提交的响应上。</p>
 */
@Component
@Slf4j
public class RestAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.warn("登录失败：{}", exception.getMessage());

        if (exception instanceof UsernameOrPasswordNullException) {
            ResultUtil.fail(response, Response.fail(exception.getMessage()));
            return;
        }

        if (exception instanceof BadCredentialsException) {
            ResultUtil.fail(response, Response.fail(ResponseCodeEnum.USERNAME_OR_PWD_ERROR));
            return;
        }

        if (exception instanceof DisabledException) {
            // 阶段 6 Day 51：管理员禁用账号后无法登录
            ResultUtil.fail(response, Response.fail(ResponseCodeEnum.ACCOUNT_DISABLED));
            return;
        }

        ResultUtil.fail(response, Response.fail(ResponseCodeEnum.LOGIN_FAIL));
    }
}
