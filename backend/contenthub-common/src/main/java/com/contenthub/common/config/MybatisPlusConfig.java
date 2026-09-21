package com.contenthub.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>注意：分页插件必须显式装配，否则 {@code page(...)} 不会真正分页，
 * 而是把全表查出来再在内存里截取——这是计划阶段 1「内容分页查询」最容易踩的坑。</p>
 */
@Configuration
@MapperScan("com.contenthub.common.domain.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页条数上限，防止前端传 pageSize=999999 把库拖垮
        pagination.setMaxLimit(100L);

        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
