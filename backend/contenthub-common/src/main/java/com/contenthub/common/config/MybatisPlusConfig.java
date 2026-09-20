package com.contenthub.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.contenthub.common.domain.mapper")
public class MybatisPlusConfig {
}