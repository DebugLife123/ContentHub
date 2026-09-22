package com.contenthub.jwt.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * 登录请求体不是合法 JSON。
 *
 * <p>必须是 {@link AuthenticationException} 的子类：登录逻辑跑在 Security 过滤器链里，
 * 位置在 {@code DispatcherServlet} 之前，{@code GlobalExceptionHandler}（
 * {@code @RestControllerAdvice}）根本看不到这里的异常。把它包成 AuthenticationException，
 * 才能走到 {@code RestAuthenticationFailureHandler} 并返回统一响应体。</p>
 */
public class LoginRequestFormatException extends AuthenticationException {

    public LoginRequestFormatException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public LoginRequestFormatException(String msg) {
        super(msg);
    }
}
