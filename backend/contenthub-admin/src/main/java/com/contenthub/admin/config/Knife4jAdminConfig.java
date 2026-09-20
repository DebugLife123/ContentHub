package com.contenthub.admin.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 管理端接口文档分组（Knife4j / springdoc-openapi）。
 *
 * <p>全局 {@code OpenAPI} 信息定义在 contenthub-common 的 Knife4jConfig 中，
 * 这里只负责把 admin 包下的 Controller 归到独立分组。</p>
 */
@Configuration
@Profile("dev") // 只在 dev 环境中开启
public class Knife4jAdminConfig {

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("Admin 后台接口")
                .packagesToScan("com.contenthub.admin.controller")
                .build();
    }
}
