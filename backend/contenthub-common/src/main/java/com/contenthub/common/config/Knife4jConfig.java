package com.contenthub.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Knife4j / springdoc-openapi 配置。
 *
 * <p>Spring Boot 3 起 springfox 不再兼容，改用 springdoc-openapi 2.x；
 * 原 {@code Docket} + {@code @EnableSwagger2WebMvc} 的写法替换为
 * {@link OpenAPI} + {@link GroupedOpenApi}。</p>
 *
 * <p>文档地址：{@code http://127.0.0.1:8084/doc.html}</p>
 */
@Configuration
@Profile("dev") // 只在 dev 环境中开启
public class Knife4jConfig {

    /**
     * 全局 API 文档信息（只需定义一次）
     */
    @Bean
    public OpenAPI contentHubOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("ContentHub 接口文档")
                .description("ContentHub 是一个数字内容订阅与创作者平台：创作者发布内容 → 用户订阅 → 按订阅获得访问权限 → 阅读/观看/下载 → 产生互动。")
                .contact(new Contact().name("ContentHub").url("https://github.com/").email("contenthub@example.com"))
                .version("1.0"));
    }

    /**
     * Web 前台接口分组
     */
    @Bean
    public GroupedOpenApi webApi() {
        return GroupedOpenApi.builder()
                .group("Web 前台接口")
                .packagesToScan("com.contenthub.web.controller")
                .build();
    }
}
