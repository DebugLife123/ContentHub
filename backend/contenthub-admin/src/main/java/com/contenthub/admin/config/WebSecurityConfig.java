package com.contenthub.admin.config;

import com.contenthub.jwt.config.JwtAuthenticationSecurityConfig;
import com.contenthub.jwt.filter.TokenAuthenticationFilter;
import com.contenthub.jwt.handler.RestAccessDeniedHandler;
import com.contenthub.jwt.handler.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置（Spring Security 6 / Spring Boot 3）。
 *
 * <p>Security 6 起 {@code WebSecurityConfigurerAdapter} 已被移除，
 * 改为直接声明 {@link SecurityFilterChain} Bean；同时链式 DSL 改为 Lambda DSL，
 * {@code mvcMatchers} 被 {@code requestMatchers} 取代。</p>
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    private JwtAuthenticationSecurityConfig jwtAuthenticationSecurityConfig;
    @Autowired
    private RestAuthenticationEntryPoint authEntryPoint;
    @Autowired
    private RestAccessDeniedHandler deniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable) // 禁用 csrf
                .formLogin(AbstractHttpConfigurer::disable) // 禁用表单登录
                .apply(jwtAuthenticationSecurityConfig) // 设置用户登录认证相关配置
                .and()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").authenticated() // 认证所有以 /admin 为前缀的 URL 资源
                        .anyRequest().permitAll()) // 其他都需要放行，无需认证
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authEntryPoint) // 未登录访问受保护资源
                        .accessDeniedHandler(deniedHandler)) // 已登录但权限不足
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 前后端分离，无需创建会话
                .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class); // Token 校验过滤器

        return http.build();
    }

    /**
     * Token 校验过滤器
     *
     * @return TokenAuthenticationFilter
     */
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter();
    }
}
