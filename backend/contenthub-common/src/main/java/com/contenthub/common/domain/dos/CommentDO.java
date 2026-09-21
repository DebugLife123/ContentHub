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
 * 评论（阶段 5 Day 45：新增、查询、删除自己的评论）。
 *
 * <p>表上没有唯一索引，因此可以安全使用逻辑删除。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("comments")
public class CommentDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contentId;

    private Long userId;

    /** 父评论ID，楼中楼用；阶段 5 暂只支持一级评论 */
    private Long parentId;

    private String body;

    /** NORMAL 正常 / HIDDEN 已隐藏（管理员操作） */
    private String status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
