package com.contenthub.web.model.req;

import lombok.Data;

import java.io.Serializable;

/** Skill 列表查询条件 */
@Data
public class SkillPageReqVO implements Serializable {

    private Long pageNum = 1L;

    private Long pageSize = 12L;

    private Long categoryId;

    /** 匹配名称 / 简介 / 作者 / 仓库 / 标签 */
    private String keyword;

    /** stars 星数优先（默认）/ updated 最近更新 / name 按名称 */
    private String sort;

    /** 仅管理端使用：DRAFT / PUBLISHED / OFFLINE，为空表示不限 */
    private String status;
}
