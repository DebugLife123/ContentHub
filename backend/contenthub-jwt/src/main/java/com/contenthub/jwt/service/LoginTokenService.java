package com.contenthub.jwt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录 Token 的 Redis 存储（计划 Day 16：登录成功后把 token 写入 Redis）。
 *
 * <p>为什么要放进 Redis，而不是只靠 JWT 自带的过期时间？</p>
 * <ul>
 *   <li>JWT 一旦签发就无法撤回——用户点「退出登录」后，旧 token 在过期前依然能用；</li>
 *   <li>把 token 作为 Redis key 存一份，退出时删除即可立即失效；</li>
 *   <li>顺带可以查 TTL、统计在线人数、在需要时踢人下线。</li>
 * </ul>
 *
 * <p>Key 形如 {@code login:token:{token}}，Value 为用户名，TTL 与 JWT 过期时间一致。</p>
 */
@Service
public class LoginTokenService {

    /** 计划表 8 约定的 Key 前缀 */
    private static final String KEY_PREFIX = "login:token:";

    private final StringRedisTemplate redisTemplate;

    /** Token 有效期（分钟），与 JWT 共用同一份配置，避免两者不一致 */
    @Value("${jwt.tokenExpireTime}")
    private long tokenExpireMinutes;

    public LoginTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 登录成功后写入，并设置过期时间 */
    public void save(String token, String username) {
        redisTemplate.opsForValue().set(key(token), username, Duration.ofMinutes(tokenExpireMinutes));
    }

    /** 该 token 是否仍然有效（未被登出、未过期） */
    public boolean isActive(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(token)));
    }

    /** 取 token 对应的用户名 */
    public String getUsername(String token) {
        return redisTemplate.opsForValue().get(key(token));
    }

    /** 剩余有效期（秒）；-2 表示 key 不存在，-1 表示未设置过期 */
    public long getTtlSeconds(String token) {
        Long ttl = redisTemplate.getExpire(key(token));
        return ttl == null ? -2L : ttl;
    }

    /** 退出登录：立即失效 */
    public boolean remove(String token) {
        return Boolean.TRUE.equals(redisTemplate.delete(key(token)));
    }

    /** Token 有效期（分钟），用于返回给前端 */
    public long getTokenExpireMinutes() {
        return tokenExpireMinutes;
    }

    private String key(String token) {
        return KEY_PREFIX + token;
    }
}
