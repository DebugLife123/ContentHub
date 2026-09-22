package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 列表项。
 *
 * <p>前置条件是 Skill 已上架；未上架的状态只有管理端能看到。</p>
 *
 * <p><b>注意这里没有 {@code locked} / {@code installCommand}。</b>
 * 列表页只展示「需不需要会员」这件事，具体能不能安装由详情接口判定——
 * 把安装命令放进列表响应，等于把付费内容随手发给了所有访客。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillListVO implements Serializable {

    private Long id;
    private String name;
    private String icon;
    private Long categoryId;
    private String categoryName;
    private String summary;
    private String author;
    /** owner/name */
    private String repo;
    private Integer stars;
    private String version;
    /** FREE 免费 / MEMBER 会员解锁 */
    private String accessType;
    /** DRAFT / PUBLISHED / OFFLINE */
    private String status;
    private List<String> platforms;
    private List<String> tags;
    private LocalDateTime updateTime;
}
