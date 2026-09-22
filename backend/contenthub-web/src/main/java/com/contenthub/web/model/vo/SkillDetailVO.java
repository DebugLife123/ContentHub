package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 详情。
 *
 * <p>{@code locked = true} 时，{@code installCommand} 与 {@code quickStart}
 * 一律不下发——与内容库「未解锁只给试读片段」是同一套做法：
 * 权限判断放服务端，前端只负责渲染结果。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SkillDetailVO implements Serializable {

    private Long id;
    private String name;
    private String icon;
    private Long categoryId;
    private String categoryName;
    private String summary;

    private String author;
    private String repo;
    private String officialUrl;
    /** locked=true 时为空字符串 */
    private String installCommand;

    private Integer stars;
    private String version;
    private String license;
    private String size;
    private Integer downloads;

    private Integer securityLevel;
    private String securityLabel;

    private String submitter;
    private LocalDate submitTime;

    private List<String> platforms;
    private List<String> tags;
    private List<String> features;
    private String whyIncluded;
    /** locked=true 时为空列表 */
    private List<SkillStepVO> quickStart;

    private SkillTeamVO team;

    /** FREE 免费 / MEMBER 会员解锁 */
    private String accessType;
    /** DRAFT / PUBLISHED / OFFLINE */
    private String status;

    /** 是否因会员权限被锁 */
    private Boolean locked;
    /** 被锁原因，用于前端展示订阅引导 */
    private String lockReason;

    private LocalDateTime updateTime;
}
