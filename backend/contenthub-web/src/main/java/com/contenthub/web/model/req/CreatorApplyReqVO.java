package com.contenthub.web.model.req;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 提交创作者申请 */
@Data
public class CreatorApplyReqVO implements Serializable {

    /** 申请说明：打算发布什么内容、有什么相关背景，给管理员判断用 */
    @Size(max = 500, message = "申请说明不能超过 500 个字符")
    private String intro;
}
