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
 * Spring Security 6 配置（阶段 2 角色授权 + 阶段 3/4 新接口）。
 *
 * <p>路径匹配是相对 {@code server.servlet.context-path} 的，因此这里写 {@code /contents}，
 * 而外部访问地址是 {@code /api/contents}。</p>
 *
 * <p><b>规则自上而下，先匹配先生效</b>，所以「需要登录」的规则必须写在
 * 「公开 GET」之前，否则会被后者覆盖掉。</p>
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

                        // ---------- 需登录或更高角色（必须在下面「公开 GET」之前） ----------
                        .requestMatchers(HttpMethod.GET, "/contents/mine", "/contents/mine/**").hasAnyRole("CREATOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/categories/all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/plans/mine").hasAnyRole("CREATOR", "ADMIN")
                        // 申请创作者身份：普通用户也要能调，所以是 authenticated 而不是 CREATOR
                        .requestMatchers(HttpMethod.POST, "/creator/apply").authenticated()
                        .requestMatchers(HttpMethod.GET, "/creator/application").authenticated()
                        // 收藏：登录即可，但必须写在 DELETE /contents/** 之前，否则会被创作者角色规则拦掉
                        .requestMatchers(HttpMethod.POST, "/contents/*/favorite").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/contents/*/favorite").authenticated()
                        // 评论与阅读进度：登录即可
                        .requestMatchers(HttpMethod.POST, "/contents/*/comments").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/contents/*/progress").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/comments/*").authenticated()
                        // 状态流转：提交审核与下架都只限作者本人（Service 里再校验归属）
                        .requestMatchers(HttpMethod.POST, "/contents/*/submit", "/contents/*/offline").hasAnyRole("CREATOR", "ADMIN")
                        .requestMatchers("/users/me", "/users/me/**").authenticated()
                        // 站内通知：只能看自己的，归属在 Service 里再校验一次
                        .requestMatchers("/notifications", "/notifications/**").authenticated()
                        // 文件上传：登录即可（头像、封面、正文附件都要用）
                        .requestMatchers(HttpMethod.POST, "/files/upload").authenticated()
                        // Skill 安装计数：必须登录，写在下面 GET /skills/** 放行之前
                        .requestMatchers(HttpMethod.POST, "/skills/*/install").authenticated()
                        // Skill 评论：发评论要登录，删评论本人或管理员（归属在 Service 里校验）
                        .requestMatchers(HttpMethod.POST, "/skills/*/comments").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/skill-comments/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/skill-comments/mine").authenticated()
                        .requestMatchers("/auth/logout").authenticated()
                        .requestMatchers("/subscriptions/**").authenticated()

                        // ---------- 公开读接口 ----------
                        .requestMatchers(HttpMethod.GET, "/contents", "/contents/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/plans").permitAll()
                        .requestMatchers(HttpMethod.GET, "/creators/**").permitAll()
                        // Skill 商城：读公开，写操作走下面的 /admin/** 规则
                        .requestMatchers(HttpMethod.GET, "/skills", "/skills/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/skill-categories").permitAll()
                        // 已上传的文件：正文里的图片/附件要能被匿名访客加载
                        .requestMatchers(HttpMethod.GET, "/files/**").permitAll()

                        // ---------- 分类写操作仅管理员 ----------
                        .requestMatchers(HttpMethod.POST, "/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/categories/**").hasRole("ADMIN")

                        // ---------- 内容写操作需要创作者或管理员（计划 Day 22） ----------
                        .requestMatchers(HttpMethod.POST, "/contents").hasAnyRole("CREATOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/contents/**").hasAnyRole("CREATOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/contents/**").hasAnyRole("CREATOR", "ADMIN")

                        // ---------- 套餐写操作需要创作者或管理员（计划 Day 31） ----------
                        .requestMatchers(HttpMethod.POST, "/plans").hasAnyRole("CREATOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/plans/**").hasAnyRole("CREATOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/plans/**").hasAnyRole("CREATOR", "ADMIN")

                        // ---------- 创作者后台 / 管理后台（计划 Day 55） ----------
                        .requestMatchers("/creator/**").hasAnyRole("CREATOR", "ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // ---------- 其余（API 文档、静态资源）放行 ----------
                        .anyRequest().permitAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authEntryPoint) // 401：未登录 / token 失效
                        .accessDeniedHandler(deniedHandler))      // 403：已登录但角色不够
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
