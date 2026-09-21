package com.contenthub.web.service;

import com.contenthub.common.domain.dos.ContentDO;

/**
 * 内容详情的 Redis 缓存（阶段 5 Day 39-40）。
 *
 * <p>只缓存 {@link ContentDO} 这一层「与访问者无关」的数据。
 * 之所以不直接缓存组装好的详情 VO：VO 里带 {@code locked} 与
 * {@code favorited}，是随访问者变化的——缓存它会把 A 用户的解锁状态泄漏给 B 用户。</p>
 */
public interface ContentCacheService {

    /** 命中则返回内容，未命中返回 null */
    ContentDO get(Long contentId);

    /** 写入缓存并设置 TTL */
    void put(ContentDO content);

    /**
     * 失效缓存。
     *
     * <p>计划 Day 40 的核心：「改了数据库但缓存还是旧数据」——
     * 因此所有写路径（新增/编辑/删除/状态流转/浏览量落库）都必须调用它。</p>
     */
    void evict(Long contentId);

    /** 剩余 TTL（秒），-2 表示 key 不存在 */
    long ttlSeconds(Long contentId);
}
