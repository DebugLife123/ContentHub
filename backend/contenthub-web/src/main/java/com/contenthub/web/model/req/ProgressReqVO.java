package com.contenthub.web.model.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/** 阅读进度（阶段 5 Day 46） */
@Data
public class ProgressReqVO implements Serializable {

    @NotNull(message = "阅读进度不能为空")
    @Min(value = 0, message = "进度不能小于 0")
    @Max(value = 100, message = "进度不能大于 100")
    private Integer progress;
}
