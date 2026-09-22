package com.contenthub.web.model.req;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 内容分页查询条件（计划表 19：GET /api/contents/page）。
 *
 * <p>pageNum / pageSize 与校验来自 {@link PageReqVO}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContentPageReqVO extends PageReqVO implements Serializable {

    /** 按分类筛选 */
    private Long categoryId;

    /** 按创作者筛选，用于创作者公开主页列出他的已发布内容 */
    private Long creatorId;

    /** 按内容类型筛选，如 PROMPT */
    private String contentType;

    /** 标题 / 摘要关键词 */
    private String keyword;

    /** 按状态筛选，仅创作者查看自己的内容时有效 */
    private String status;
}
