package com.contenthub.web.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** 创作者仪表盘统计（阶段 6 Day 48：内容数、阅读量、收藏量） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorDashboardVO implements Serializable {

    /** 全部内容数 */
    private long contentCount;
    /** 各状态数量 */
    private long publishedCount;
    private long draftCount;
    private long pendingCount;
    private long rejectedCount;
    private long offlineCount;

    /** 所有内容的阅读量合计 */
    private long totalViews;
    /** 收藏数合计 */
    private long totalFavorites;
    /** 评论数合计 */
    private long totalComments;

    /** 有效订阅人数（去重） */
    private long subscriberCount;
    /** 套餐数 */
    private long planCount;
}
