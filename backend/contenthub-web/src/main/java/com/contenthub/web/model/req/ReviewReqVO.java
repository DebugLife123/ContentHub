package com.contenthub.web.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 管理员审核驳回（计划 Day 23） */
@Data
public class ReviewReqVO implements Serializable {

    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 500, message = "驳回原因不能超过 500 个字符")
    private String reason;
}
