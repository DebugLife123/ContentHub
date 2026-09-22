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

import java.time.LocalDateTime;

/**
 * Skill 分类（对应 skill_category 表）。
 *
 * <p>与内容库的 {@code content_category} 相互独立：内容库按「内容形态」划分，
 * Skill 商城按「能力领域」划分，两套栏目没有从属关系。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("skill_category")
public class SkillCategoryDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称，数据库层唯一 */
    private String name;

    /** 排序值，越小越靠前 */
    private Integer sort;

    /** ENABLED 启用 / DISABLED 禁用 */
    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
