package com.contenthub.common.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收藏（计划 Day 28：唯一约束、防重复收藏）。
 *
 * <p>表上有 {@code uk_user_content(user_id, content_id)}，数据库层就挡得住重复收藏。</p>
 *
 * <p><b>这里刻意不加 {@code @TableLogic}</b>：逻辑删除后行仍然占着那个唯一索引，
 * 于是「取消收藏 → 重新收藏」会在 INSERT 时撞唯一键报 500。
 * 收藏是一条轻量关系记录，取消即物理删除更合理。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("favorites")
public class FavoriteDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long contentId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("is_deleted")
    private Boolean deleted;
}
