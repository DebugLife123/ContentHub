package com.contenthub.web.model.req;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Skill 列表查询条件。
 *
 * <p>pageNum / pageSize 与校验来自 {@link PageReqVO}——这两个字段以前是各写各的，
 * Skill 这边漏了校验，导致 pageNum 可以传负数、pageSize 可以传成任意大。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SkillPageReqVO extends PageReqVO implements Serializable {

    private Long categoryId;

    /** 匹配名称 / 简介 / 作者 / 仓库 / 标签 */
    private String keyword;

    /** stars 星数优先（默认）/ updated 最近更新 / name 按名称 */
    private String sort;

    /** 仅管理端使用：DRAFT / PUBLISHED / OFFLINE，为空表示不限 */
    private String status;
}
