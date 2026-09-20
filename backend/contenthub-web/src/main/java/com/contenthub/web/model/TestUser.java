package com.contenthub.web.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Schema(description = "用户实体类")
public class TestUser {
    // 用户名
    @NotBlank
    private String username;
    // 性别
    @Max(100)
    @Min(0)
    private Integer sex;

    // 创建时间
    private LocalDateTime createTime;
    // 更新日期
    private LocalDate updateDate;
    // 时间
    private LocalTime time;
}
