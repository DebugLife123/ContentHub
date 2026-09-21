package com.contenthub.admin.config;

import com.contenthub.jwt.filter.JwtAuthenticationFilter;
import com.contenthub.jwt.filter.TokenAuthenticationFilter;
import com.contenthub.jwt.handler.RestAccessDeniedHandler;
import com.contenthub.jwt.handler.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 6 配置（阶段 2：登录与权限）。
 *
 * <p>Security 6 起 {@code WebSecurityConfigurerAdapter} 已被移除，改为直接声明
 * {@link SecurityFilterChain} Bean；链式 DSL 改为 Lambda DSL，
 * {@code mvcMatchers} 被 {@code requestMatchers} 取代。</p>
 *
 * <p>路径匹配是相对 {@code server.servlet.context-path} 的，因此这里写 {@code /contents}，
 * 而外部访问地址是 {@code /api/contents}。规则自上而下，先匹配先生效。</p>
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final RestAuthenticationEntryPoint authEntryPoint;
    private final RestAccessDeniedHandler deniedHandler;

    public WebSecurityConfig(RestAuthenticationEntryPoint authEntryPoint,
                             RestAccessDeniedHandler deniedHandler) {
        this.authEntryPoint = authEntryPoint;
        this.deniedHandler = deniedHandler;
    }

    /**
     * 认证管理器：只使用「数据库用户名 + 密码」这一种方式。
     */
    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider daoAuthenticationProvider) {
        return new ProviderManager(daoAuthenticationProvider);
    }

    /**
     * Token 校验过滤器。
     *
     * <p>必须作为 Bean 交给 Spring 管理——它的依赖都用 {@code @Autowired} 字段注入，
     * 直接 {@code new} 出来会得到一个所有依赖都为 null 的实例。</p>
     */
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           TokenAuthenticationFilter tokenAuthenticationFilter,
                                           DaoAuthenticationProvider daoAuthenticationProvider) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authenticationProvider(daoAuthenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        // ---------- 公开：认证接口 ----------
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register").permitAll()

                        // ---------- 需要登录（必须写在下面的公开 GET 规则之前，先匹配先生效） ----------
                        .requestMatchers(HttpMethod.GET, "/contents/mine", "/contents/mine/**").hasAnyRole("CREATOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/categories/all").hasRole("ADMIN")

                        // ---------- 公开：内容与分类的读接口（游客可浏览） ----------
                        .requestMatchers(HttpMethod.GET, "/contents", "/contents/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories").permitAll()

                        // ---------- 分类写操作仅管理员 ----------
                        .requestMatchers(HttpMethod.POST, "/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/categories/**").hasRole("ADMIN")

                        // ---------- 内容写操作需要创作者或管理员（计划 Day 22） ----------
                        .requestMatchers(HttpMethod.POST, "/contents").hasAnyRole("CREATOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/contents/**").hasAnyRole("CREATOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/contents/**").hasAnyRole("CREATOR", "ADMIN")

                        // ---------- 需要登录 ----------
                        .requestMatchers("/users/me", "/auth/logout").authenticated()

                        // ---------- 创作者后台 / 管理后台（计划 Day 55） ----------
                        .requestMatchers("/creator/**").hasAnyRole("CREATOR", "ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // ---------- 其余（API 文档、静态资源、脚手架接口）放行 ----------
                        .anyRequest().permitAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authEntryPoint) // 401：未登录 / token 失效
                        .accessDeniedHandler(deniedHandler))      // 403：已登录但角色不够
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 前后端分离，不建会话
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
