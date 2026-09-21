package com.contenthub.web.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/** 新增 / 编辑内容的请求体 */
@Data
public class ContentReqVO implements Serializable {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过 200 个字符")
    private String title;

    @Size(max = 500, message = "摘要长度不能超过 500 个字符")
    private String summary;

    @Size(max = 255, message = "封面地址过长")
    private String cover;

    @NotBlank(message = "内容类型不能为空")
    private String contentType;

    /** 分类ID，可为空 */
    private Long categoryId;

    /** 正文，可为空（例如只提供附件的内容） */
    private String body;

    @Size(max = 500, message = "附件地址过长")
    private String fileUrl;

    /** FREE / SUBSCRIBED；为空时新增默认 FREE */
    private String accessType;

    /** DRAFT / PUBLISHED / OFFLINE；为空时新增默认 DRAFT */
    private String status;
}
