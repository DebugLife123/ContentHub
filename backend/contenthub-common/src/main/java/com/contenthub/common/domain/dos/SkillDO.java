package com.contenthub.common.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Skill（对应 skill 表）。
 *
 * <p>Skill 由管理员在管理端维护，没有创作者投稿与审核环节，因此状态只有
 * {@code DRAFT / PUBLISHED / OFFLINE} 三态。</p>
 *
 * <p>几个「列表型」字段在数据库里是字符串，转换统一放在 Service 层，DO 保持与表一一对应：</p>
 * <ul>
 *   <li>{@code platforms} / {@code tags}：英文逗号分隔</li>
 *   <li>{@code features}：一行一条</li>
 *   <li>{@code quickStart}：JSON 数组 {@code [{"title":..,"detail":..}]}</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("skill")
public class SkillDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 名称，数据库层唯一 */
    private String name;

    /** 图标（emoji） */
    private String icon;

    private Long categoryId;

    private String summary;

    /** 为什么收录 */
    private String whyIncluded;

    /** 上游作者 */
    private String author;

    /** 仓库，形如 owner/name */
    private String repo;

    private String officialUrl;

    private String installCommand;

    /** GitHub 星数，列表默认排序依据 */
    private Integer stars;

    private String version;

    private String license;

    /** 体积，形如 4.0 MB */
    private String size;

    private Integer downloads;

    private Integer securityLevel;

    private String securityLabel;

    private String submitter;

    private LocalDate submitTime;

    /** 兼容平台，逗号分隔 */
    private String platforms;

    /** 标签，逗号分隔 */
    private String tags;

    /** 功能特点，一行一条 */
    private String features;

    /** 快速上手步骤，JSON 数组 */
    private String quickStart;

    private Integer teamMaintainers;

    private Integer teamContributors;

    private Integer teamOpenIssues;

    private LocalDate teamLastCommit;

    /** FREE 免费 / MEMBER 会员解锁 */
    private String accessType;

    /** DRAFT 草稿 / PUBLISHED 已上架 / OFFLINE 已下架 */
    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
