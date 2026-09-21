package com.contenthub.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan({"com.contenthub.*"})
// 阶段 5 Day 43：浏览量定时同步需要开启调度
@EnableScheduling
public class ContenthubWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContenthubWebApplication.class, args);
    }

}
