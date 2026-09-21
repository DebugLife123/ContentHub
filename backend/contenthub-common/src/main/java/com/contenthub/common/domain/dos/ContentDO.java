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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("contents")
public class ContentDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创作者ID -> users.id */
    private Long creatorId;

    /** 分类ID -> content_category.id，计划表 7 要求 */
    private Long categoryId;

    private String title;

    private String summary;

    private String cover;

    /** ARTICLE/TUTORIAL/EBOOK/VIDEO/PDF/CODE/PROMPT/DATASET/COLUMN */
    private String contentType;

    private String body;

    private String fileUrl;

    /** FREE 免费 / SUBSCRIBED 订阅可见 */
    private String accessType;

    /** DRAFT / PENDING / PUBLISHED / REJECTED / OFFLINE */
    private String status;

    private Integer viewCount;

    private Integer likeCount;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除：加了 @TableLogic 后查询自动过滤，deleteById 自动变为 UPDATE */
    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
