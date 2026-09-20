package com.contenthub.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.contenthub.*"})
public class ContenthubWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContenthubWebApplication.class, args);
    }

}
