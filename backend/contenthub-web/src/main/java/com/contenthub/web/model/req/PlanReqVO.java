package com.contenthub.web.model.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 新增 / 编辑订阅套餐（计划 Day 30-31） */
@Data
public class PlanReqVO implements Serializable {

    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 100, message = "套餐名称不能超过 100 个字符")
    private String name;

    @Size(max = 500, message = "套餐描述不能超过 500 个字符")
    private String description;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能为负")
    private BigDecimal price;

    @NotNull(message = "有效天数不能为空")
    @Min(value = 1, message = "有效天数至少为 1 天")
    private Integer durationDays;

    /** ACTIVE / INACTIVE，为空时新增默认 ACTIVE */
    private String status;
}
