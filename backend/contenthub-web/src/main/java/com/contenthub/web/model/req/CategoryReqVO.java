package com.contenthub.web.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 新增 / 编辑分类的请求体 */
@Data
public class CategoryReqVO implements Serializable {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 60, message = "分类名称不能超过 60 个字符")
    private String name;

    /** 排序值，越小越靠前；为空默认 0 */
    private Integer sort;

    /** ENABLED / DISABLED；为空时新增默认 ENABLED */
    private String status;
}
