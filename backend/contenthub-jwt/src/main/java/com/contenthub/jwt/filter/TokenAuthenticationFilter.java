package com.contenthub.jwt.filter;

import com.contenthub.jwt.model.LoginUser;
import com.contenthub.jwt.service.LoginTokenService;
import com.contenthub.jwt.utils.JwtTokenHelper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

/**
 * 每次请求校验 Authorization 头里的 JWT。
 *
 * <p>阶段 2 在原有「校验签名与过期时间」之上增加了 Redis 校验：
 * token 必须在 Redis 中存在才算有效。这样退出登录后旧 token 立即失效，
 * 而不是等它自然过期（纯 JWT 是签发即不可撤回的）。</p>
 */
@Slf4j
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.tokenPrefix}")
    private String tokenPrefix;

    @Value("${jwt.tokenHeaderKey}")
    private String tokenHeaderKey;

    @Autowired
    private JwtTokenHelper jwtTokenHelper;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private LoginTokenService loginTokenService;

    @Autowired
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(tokenHeaderKey);

        if (StringUtils.startsWith(header, tokenPrefix)) {
            // 去掉 "Bearer " 前缀
            String token = StringUtils.substring(header, 7);

            if (StringUtils.isNotBlank(token)) {
                try {
                    jwtTokenHelper.validateToken(token);
                } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
                    authenticationEntryPoint.commence(request, response, new AuthenticationServiceException("Token 不可用"));
                    return;
                } catch (ExpiredJwtException e) {
                    authenticationEntryPoint.commence(request, response, new AuthenticationServiceException("Token 已失效"));
                    return;
                }

                // Redis 中不存在 => 已退出登录（或被服务端主动失效）
                if (!loginTokenService.isActive(token)) {
                    authenticationEntryPoint.commence(request, response,
                            new AuthenticationServiceException("登录状态已失效，请重新登录"));
                    return;
                }

                String username = jwtTokenHelper.getUsernameByToken(token);
                if (StringUtils.isNotBlank(username)
                        && Objects.isNull(SecurityContextHolder.getContext().getAuthentication())) {

                    var userDetails = userDetailsService.loadUserByUsername(username);

                    // 账号被管理员禁用后必须立刻挡住：token 在 Redis 里仍然存在，
                    // 只靠登录时的 DisabledException 拦不住「禁用前签发、禁用后继续用」的旧 token。
                    // 这里每次都从库里重新加载 UserDetails，所以 isEnabled() 是当前真实状态。
                    if (!userDetails.isEnabled()) {
                        authenticationEntryPoint.commence(request, response,
                                new AuthenticationServiceException("账号已被禁用，请联系管理员"));
                        return;
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    if (log.isDebugEnabled() && userDetails instanceof LoginUser loginUser) {
                        log.debug("已认证用户 {}，角色 {}", loginUser.getUsername(), loginUser.getRole());
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
