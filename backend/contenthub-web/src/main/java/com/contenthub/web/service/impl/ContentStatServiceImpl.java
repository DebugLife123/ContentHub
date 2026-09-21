package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.contenthub.common.constants.RedisKeys;
import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.domain.mapper.ContentMapper;
import com.contenthub.web.service.ContentCacheService;
import com.contenthub.web.service.ContentStatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class ContentStatServiceImpl implements ContentStatService {

    /** 收藏的热度权重，高于一次浏览 */
    private static final double FAVORITE_WEIGHT = 3.0;

    private final StringRedisTemplate redisTemplate;
    private final ContentMapper contentMapper;
    private final ContentCacheService contentCacheService;

    public ContentStatServiceImpl(StringRedisTemplate redisTemplate,
                                  ContentMapper contentMapper,
                                  ContentCacheService contentCacheService) {
        this.redisTemplate = redisTemplate;
        this.contentMapper = contentMapper;
        this.contentCacheService = contentCacheService;
    }

    @Override
    public void recordView(Long contentId) {
        if (contentId == null) {
            return;
        }
        // Day 42：浏览量 INCR
        redisTemplate.opsForValue().increment(RedisKeys.contentView(contentId));
        // 记进待同步集合，定时任务只处理这一批，避免 SCAN 全库
        redisTemplate.opsForSet().add(RedisKeys.VIEW_DIRTY_SET, String.valueOf(contentId));
        // Day 41：ZSet 加分
        redisTemplate.opsForZSet().incrementScore(RedisKeys.HOT_CONTENT, String.valueOf(contentId), 1.0);
    }

    @Override
    public void recordFavorite(Long contentId, boolean favorited) {
        if (contentId == null) {
            return;
        }
        redisTemplate.opsForZSet().incrementScore(RedisKeys.HOT_CONTENT, String.valueOf(contentId),
                favorited ? FAVORITE_WEIGHT : -FAVORITE_WEIGHT);

        // 取消收藏可能把分数压到负数，热度不应为负，这里兜一次底
        Double score = redisTemplate.opsForZSet().score(RedisKeys.HOT_CONTENT, String.valueOf(contentId));
        if (score != null && score < 0) {
            redisTemplate.opsForZSet().add(RedisKeys.HOT_CONTENT, String.valueOf(contentId), 0);
        }
    }

    @Override
    public long pendingViews(Long contentId) {
        String value = redisTemplate.opsForValue().get(RedisKeys.contentView(contentId));
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public List<Long> hotContentIds(int limit) {
        Set<String> ids = redisTemplate.opsForZSet()
                .reverseRange(RedisKeys.HOT_CONTENT, 0, Math.max(0, limit - 1));
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            try {
                result.add(Long.parseLong(id));
            } catch (NumberFormatException ignored) {
                // 脏数据直接跳过
            }
        }
        return result;
    }

    @Override
    public double hotScore(Long contentId) {
        Double score = redisTemplate.opsForZSet().score(RedisKeys.HOT_CONTENT, String.valueOf(contentId));
        return score == null ? 0d : score;
    }

    @Override
    public int flushViewsToDatabase() {
        Set<String> dirty = redisTemplate.opsForSet().members(RedisKeys.VIEW_DIRTY_SET);
        if (dirty == null || dirty.isEmpty()) {
            return 0;
        }

        int synced = 0;
        for (String rawId : dirty) {
            Long contentId;
            try {
                contentId = Long.parseLong(rawId);
            } catch (NumberFormatException e) {
                redisTemplate.opsForSet().remove(RedisKeys.VIEW_DIRTY_SET, rawId);
                continue;
            }

            // 用 GETDEL 语义：先取走计数再累加，避免「取完没落库就崩溃」导致重复累加
            String value = redisTemplate.opsForValue().getAndDelete(RedisKeys.contentView(contentId));
            redisTemplate.opsForSet().remove(RedisKeys.VIEW_DIRTY_SET, rawId);

            long delta;
            try {
                delta = value == null ? 0L : Long.parseLong(value);
            } catch (NumberFormatException e) {
                continue;
            }
            if (delta <= 0) {
                continue;
            }

            ContentDO content = contentMapper.selectById(contentId);
            if (content == null) {
                continue;
            }
            int current = content.getViewCount() == null ? 0 : content.getViewCount();
            contentMapper.updateById(ContentDO.builder()
                    .id(contentId)
                    .viewCount((int) Math.min(Integer.MAX_VALUE, current + delta))
                    .build());

            // 浏览量落库后缓存里的 viewCount 就旧了，必须失效（Day 40）
            contentCacheService.evict(contentId);
            synced++;
        }

        if (synced > 0) {
            log.info("浏览量同步完成：{} 条", synced);
        }
        return synced;
    }

    @Override
    public void warmUpHotRanking() {
        Long size = redisTemplate.opsForZSet().zCard(RedisKeys.HOT_CONTENT);
        if (size != null && size > 0) {
            return;
        }

        // 热门 ZSet 通常只反映「服务启动后」的热度；冷启动时用库里的历史 view_count
        // 打底，否则刚启动时热门榜是空的、看起来像功能坏了
        List<ContentDO> published = contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                .select(ContentDO::getId, ContentDO::getViewCount, ContentDO::getLikeCount)
                .eq(ContentDO::getStatus, "PUBLISHED"));

        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();
        for (ContentDO c : published) {
            double score = (c.getViewCount() == null ? 0 : c.getViewCount())
                    + (c.getLikeCount() == null ? 0 : c.getLikeCount()) * 2.0;
            zset.add(RedisKeys.HOT_CONTENT, String.valueOf(c.getId()), score);
        }
        log.info("热门榜已用数据库历史数据初始化，共 {} 条", published.size());
    }
}
