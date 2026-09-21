package com.contenthub.jwt.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    /**
     * 指定用户登录的访问地址。
     *
     * <p>计划表 19 约定认证接口为 {@code POST /api/auth/login}，其中 {@code /api} 是
     * server.servlet.context-path，因此这里匹配 context 内的 {@code /auth/login}。</p>
     */
    public JwtAuthenticationFilter() {
        super(new AntPathRequestMatcher("/auth/login","POST"));
    }
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        ObjectMapper mapper = new ObjectMapper();
       //解析提交的JSON数据
       JsonNode jsonNode = mapper.readTree(request.getInputStream());
       JsonNode usernameNode = jsonNode.get("username");
       JsonNode passwordNode = jsonNode.get("password");

       //判断用户名、密码是否为空
        if (Objects.isNull(usernameNode) || Objects.isNull(passwordNode)
            || StringUtils.isBlank(usernameNode.textValue()) || StringUtils.isBlank(passwordNode.textValue())) {
            throw new UsernameOrPasswordNullException("用户名或密码不能为空");
        }
        String username = usernameNode.textValue();
        String password = passwordNode.textValue();

        //将用户名、密码封装到Token中
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                = new UsernamePasswordAuthenticationToken(username,password);
        return getAuthenticationManager().authenticate(usernamePasswordAuthenticationToken);
    }
}
