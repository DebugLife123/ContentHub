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
 * 阅读记录（阶段 5 Day 46：完成历史记录与最近阅读）。
 *
 * <p><b>刻意不加 {@code @TableLogic}</b>：表上有 {@code uk_user_content}，
 * 逻辑删除会让已删除行继续占用唯一键，导致同一用户再次阅读同一篇内容时
 * INSERT 撞唯一键。这里用「查不到就插入、查到就更新」的 upsert 写法，
 * 不做删除。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reading_history")
public class ReadingHistoryDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long contentId;

    /** 阅读进度百分比 0-100 */
    private Integer progress;

    @TableField("last_read_time")
    private LocalDateTime lastReadTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("is_deleted")
    private Boolean deleted;
}
