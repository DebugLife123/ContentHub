package com.contenthub.web.service.impl;

import com.contenthub.common.constants.RedisKeys;
import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.web.service.ContentCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class ContentCacheServiceImpl implements ContentCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ContentCacheServiceImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ContentDO get(Long contentId) {
        if (contentId == null) {
            return null;
        }
        String json = redisTemplate.opsForValue().get(RedisKeys.content(contentId));
        if (json == null) {
            log.debug("内容缓存未命中：id={}", contentId);
            return null;
        }
        try {
            ContentDO content = objectMapper.readValue(json, ContentDO.class);
            log.debug("内容缓存命中：id={}", contentId);
            return content;
        } catch (Exception e) {
            // 缓存内容损坏（例如类结构变更后旧 JSON 反序列化失败）：删掉当作未命中，
            // 而不是把异常抛给调用方导致接口 500
            log.warn("内容缓存反序列化失败，已清除：id={}", contentId, e);
            evict(contentId);
            return null;
        }
    }

    @Override
    public void put(ContentDO content) {
        if (content == null || content.getId() == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(content);
            redisTemplate.opsForValue().set(RedisKeys.content(content.getId()), json,
                    Duration.ofMinutes(RedisKeys.CONTENT_CACHE_MINUTES));
        } catch (Exception e) {
            // 缓存写入失败不应影响主流程
            log.warn("内容缓存写入失败：id={}", content.getId(), e);
        }
    }

    @Override
    public void evict(Long contentId) {
        if (contentId == null) {
            return;
        }
        Boolean removed = redisTemplate.delete(RedisKeys.content(contentId));
        if (Boolean.TRUE.equals(removed)) {
            log.debug("内容缓存已失效：id={}", contentId);
        }
    }

    @Override
    public long ttlSeconds(Long contentId) {
        Long ttl = redisTemplate.getExpire(RedisKeys.content(contentId));
        return ttl == null ? -2L : ttl;
    }
}
