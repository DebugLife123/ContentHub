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
 * 内容分类（对应 content_category 表）。
 *
 * <p>计划表 6 把 content_category 标记为「必须」。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("content_category")
public class ContentCategoryDO {

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

    /** 逻辑删除：加了 @TableLogic 后，查询自动过滤、deleteById 自动变为 UPDATE */
    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
