package com.contenthub.jwt.config;

import com.contenthub.jwt.filter.JwtAuthenticationFilter;
import com.contenthub.jwt.handler.RestAuthenticationFailureHandler;
import com.contenthub.jwt.handler.RestAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 登录认证相关 Bean。
 *
 * <p>改造前这里继承 {@code SecurityConfigurerAdapter} 并通过 {@code http.apply(...)} 注册，
 * 该 API 在 Spring Security 6 已标记「废弃并将在后续版本删除」，编译时会告警。
 * 现在改为直接暴露 {@link DaoAuthenticationProvider} 与 {@link JwtAuthenticationFilter}
 * 两个 Bean，由 {@code WebSecurityConfig} 显式装配，不再依赖已废弃的扩展点。</p>
 */
@Configuration
public class JwtAuthenticationSecurityConfig {

    /**
     * 用户名 + 密码的认证提供者。
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
                                                               PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * 处理 {@code POST /auth/login} 的过滤器。
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(AuthenticationManager authenticationManager,
                                                           RestAuthenticationSuccessHandler successHandler,
                                                           RestAuthenticationFailureHandler failureHandler) {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        filter.setAuthenticationManager(authenticationManager);
        filter.setAuthenticationSuccessHandler(successHandler);
        filter.setAuthenticationFailureHandler(failureHandler);
        return filter;
    }
}
