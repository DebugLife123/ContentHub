package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 分类返回结构 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryVO implements Serializable {

    private Long id;
    private String name;
    private Integer sort;
    /** ENABLED / DISABLED */
    private String status;
    /** 该分类下已发布内容数 */
    private Long contentCount;
    private LocalDateTime createTime;
}
