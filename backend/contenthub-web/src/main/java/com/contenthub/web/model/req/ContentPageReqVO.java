package com.contenthub.web.model.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/** 内容分页查询条件（计划表 19：GET /api/contents/page） */
@Data
public class ContentPageReqVO implements Serializable {

    @Min(value = 1, message = "页码从 1 开始")
    private Long pageNum = 1L;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 100, message = "每页最多 100 条")
    private Long pageSize = 9L;

    /** 按分类筛选 */
    private Long categoryId;

    /** 按内容类型筛选，如 PROMPT */
    private String contentType;

    /** 标题 / 摘要关键词 */
    private String keyword;

    /** 按状态筛选，仅创作者查看自己的内容时有效 */
    private String status;
}
