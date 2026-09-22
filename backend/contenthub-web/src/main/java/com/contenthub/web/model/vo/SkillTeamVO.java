package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/** Skill 详情页右侧「团队协作」卡片 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillTeamVO implements Serializable {

    private Integer maintainers;
    private Integer contributors;
    private Integer openIssues;
    private LocalDate lastCommit;
}
