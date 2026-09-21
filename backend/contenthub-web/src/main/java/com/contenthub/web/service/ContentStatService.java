package com.contenthub.web.service;

import java.util.List;

/**
 * 浏览量、热门内容与 Redis→MySQL 的同步（阶段 5 Day 41-43）。
 */
public interface ContentStatService {

    /**
     * 记录一次内容访问：Redis 浏览量 +1，并给热门 ZSet 加分。
     *
     * <p>刻意不直接 UPDATE MySQL：浏览量是典型的「高频写、允许最终一致」数据，
     * 每次都打库会成为热点行的写竞争。</p>
     */
    void recordView(Long contentId);

    /** 收藏 / 取消收藏时调整热度分（收藏比浏览权重高） */
    void recordFavorite(Long contentId, boolean favorited);

    /** 取 Redis 中累计但尚未同步的浏览量增量 */
    long pendingViews(Long contentId);

    /** 热门内容ID，按热度分从高到低 */
    List<Long> hotContentIds(int limit);

    /** 某个内容的热度分 */
    double hotScore(Long contentId);

    /**
     * 把 Redis 里的浏览量增量同步到 MySQL（计划 Day 43 的定时任务调用）。
     *
     * @return 本次同步的内容条数
     */
    int flushViewsToDatabase();

    /** 热门 ZSet 为空时，用 MySQL 的历史 view_count 初始化一次 */
    void warmUpHotRanking();
}
