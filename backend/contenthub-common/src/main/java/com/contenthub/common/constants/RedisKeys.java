package com.contenthub.common.constants;

/**
 * Redis Key 约定。
 *
 * <p>与计划表 8「Redis 在这个项目里的 4 个练习点」一一对应，
 * 集中在一处避免各 Service 拼字符串拼错。</p>
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 登录 token：{@code login:token:{token}} -> 用户名，TTL 与 JWT 一致 */
    public static final String LOGIN_TOKEN_PREFIX = "login:token:";

    /** 内容详情缓存：{@code content:{contentId}} -> ContentDetailVO 的 JSON */
    public static final String CONTENT_PREFIX = "content:";

    /** 浏览量计数：{@code content:view:{contentId}} -> 待同步到 MySQL 的增量 */
    public static final String CONTENT_VIEW_PREFIX = "content:view:";

    /** 内容详情缓存 TTL（分钟） */
    public static final long CONTENT_CACHE_MINUTES = 30;

    /** 热门内容 ZSet：{@code hot:content}，score 为热度分 */
    public static final String HOT_CONTENT = "hot:content";

    /** 浏览量增量同步的批次标记：{@code content:view:dirty}，Set 存放待同步的 contentId */
    public static final String VIEW_DIRTY_SET = "content:view:dirty";

    public static String loginToken(String token) {
        return LOGIN_TOKEN_PREFIX + token;
    }

    public static String content(Long contentId) {
        return CONTENT_PREFIX + contentId;
    }

    public static String contentView(Long contentId) {
        return CONTENT_VIEW_PREFIX + contentId;
    }
}
