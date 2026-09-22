package com.contenthub.web.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 新增 / 编辑 Skill 分类的请求体 */
@Data
public class SkillCategoryReqVO implements Serializable {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 60, message = "分类名称长度不能超过 60 个字符")
    private String name;

    private Integer sort;

    /** ENABLED / DISABLED；为空时新增默认 ENABLED */
    private String status;
}
