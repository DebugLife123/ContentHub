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

/** 创作者资料（计划 Day 20：理解 user 与 creator 的关系） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("creator_profiles")
public class CreatorProfileDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 users.id，数据库层唯一 */
    private Long userId;

    /** 创作者展示名 */
    private String displayName;

    private String intro;

    /** 是否平台认证 */
    private Boolean verified;

    private Integer subscriberCount;

    private Integer contentCount;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
