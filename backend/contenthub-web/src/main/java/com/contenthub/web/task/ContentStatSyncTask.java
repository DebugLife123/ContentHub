package com.contenthub.web.task;

import com.contenthub.web.service.ContentStatService;
import com.contenthub.web.service.SkillStatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 阶段 5 Day 43：定时把 Redis 里的浏览量同步到 MySQL。
 *
 * <p>用 {@code fixedDelay} 而不是 {@code fixedRate}：上一次同步还没跑完时不要叠加下一次，
 * 否则同步变慢时会堆积任务。</p>
 */
@Component
@Slf4j
public class ContentStatSyncTask {

    private final ContentStatService contentStatService;
    private final SkillStatService skillStatService;

    public ContentStatSyncTask(ContentStatService contentStatService, SkillStatService skillStatService) {
        this.contentStatService = contentStatService;
        this.skillStatService = skillStatService;
    }

    /** 间隔可通过 contenthub.view-sync-interval-ms 配置，默认 30 秒 */
    @Scheduled(fixedDelayString = "${contenthub.view-sync-interval-ms:30000}",
            initialDelayString = "${contenthub.view-sync-interval-ms:30000}")
    public void syncViewCounts() {
        try {
            contentStatService.flushViewsToDatabase();
        } catch (Exception e) {
            // 定时任务抛异常会导致后续调度被取消（fixedDelay 下 Spring 会继续，
            // 但日志噪音很大），这里兜住并记录
            log.error("浏览量同步失败", e);
        }

        // Skill 安装量复用同一个任务：两件事都是「Redis 计数 -> 落库」，
        // 拆成两个任务只会让调度配置重复一份
        try {
            skillStatService.flushInstallsToDatabase();
        } catch (Exception e) {
            log.error("Skill 安装量同步失败", e);
        }
    }

    /** 应用启动后用数据库历史数据给热门榜打底 */
    @EventListener(ApplicationReadyEvent.class)
    public void initHotRanking() {
        try {
            contentStatService.warmUpHotRanking();
        } catch (Exception e) {
            log.error("热门榜初始化失败", e);
        }
    }
}
