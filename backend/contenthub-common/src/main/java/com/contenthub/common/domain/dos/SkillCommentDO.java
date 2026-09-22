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
 * Skill 评论（对应 skill_comment 表）。
 *
 * <p>内容库的 comments 绑在 content_id 上，Skill 是另一条业务线，这里单开一张表。
 * 代价是两套评论代码形态相近，收益是内容评论那条已经跑通的链路完全不受影响。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("skill_comment")
public class SkillCommentDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long skillId;

    private Long userId;

    private String body;

    /** NORMAL 正常 / HIDDEN 已隐藏 */
    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
