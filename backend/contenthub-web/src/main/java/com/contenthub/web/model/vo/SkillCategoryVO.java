package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Skill 分类 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillCategoryVO implements Serializable {

    private Long id;
    private String name;
    private Integer sort;
    /** ENABLED / DISABLED */
    private String status;
    /** 该分类下的 Skill 总数（含未上架），供管理端展示 */
    private Long skillCount;
    private LocalDateTime createTime;
}
