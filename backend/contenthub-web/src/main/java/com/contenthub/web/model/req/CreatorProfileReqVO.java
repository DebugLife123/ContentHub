package com.contenthub.web.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 创作者资料修改（计划 Day 20） */
@Data
public class CreatorProfileReqVO implements Serializable {

    @NotBlank(message = "创作者展示名不能为空")
    @Size(max = 60, message = "展示名不能超过 60 个字符")
    private String displayName;

    @Size(max = 1000, message = "介绍不能超过 1000 个字符")
    private String intro;
}
