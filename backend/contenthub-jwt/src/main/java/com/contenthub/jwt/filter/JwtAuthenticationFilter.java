package com.contenthub.jwt.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.contenthub.jwt.exception.LoginRequestFormatException;
import com.contenthub.jwt.exception.UsernameOrPasswordNullException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

public class JwtAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    /** 复用同一个 ObjectMapper，而不是每个请求 new 一个 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String FORMAT_HINT =
            "请求体格式不正确，需要 JSON，形如 {\"username\":\"admin\",\"password\":\"123456\"}";

    /**
     * 指定用户登录的访问地址。
     *
     * <p>计划表 19 约定认证接口为 {@code POST /api/auth/login}，其中 {@code /api} 是
     * server.servlet.context-path，因此这里匹配 context 内的 {@code /auth/login}。</p>
     */
    public JwtAuthenticationFilter() {
        super(new AntPathRequestMatcher("/auth/login", "POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        JsonNode jsonNode;
        try {
            jsonNode = MAPPER.readTree(request.getInputStream());
        } catch (IOException e) {
            // 表单编码、手写坏的 JSON 都会走到这里。
            // 不能直接让 IOException 抛出去：过滤器跑在 DispatcherServlet 之前，
            // GlobalExceptionHandler 兜不住，容器会返回一个不符合统一返回约定的 500。
            throw new LoginRequestFormatException(FORMAT_HINT, e);
        }

        // 空请求体时 readTree 可能返回 null 或 MissingNode，两种情况都要挡住
        JsonNode usernameNode = jsonNode == null ? null : jsonNode.get("username");
        JsonNode passwordNode = jsonNode == null ? null : jsonNode.get("password");

        // 判断用户名、密码是否为空
        if (Objects.isNull(usernameNode) || Objects.isNull(passwordNode)
                || StringUtils.isBlank(usernameNode.textValue()) || StringUtils.isBlank(passwordNode.textValue())) {
            throw new UsernameOrPasswordNullException("用户名或密码不能为空");
        }

        String username = usernameNode.textValue();
        String password = passwordNode.textValue();

        // 将用户名、密码封装到Token中
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                = new UsernamePasswordAuthenticationToken(username, password);
        return getAuthenticationManager().authenticate(usernamePasswordAuthenticationToken);
    }
}
