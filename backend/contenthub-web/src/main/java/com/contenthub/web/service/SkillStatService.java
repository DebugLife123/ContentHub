package com.contenthub.web.service;

/**
 * Skill 安装量统计。
 *
 * <p>复用内容浏览量那一套：Redis 累加 + 定时落库。安装量同样属于
 * 「高频写、允许最终一致」的数据，没必要每次点安装都去 UPDATE 同一行。</p>
 */
public interface SkillStatService {

    /** 记录一次安装：Redis 计数 +1 */
    void recordInstall(Long skillId);

    /** 取 Redis 中累计但尚未落库的增量，用于列表/详情展示「最新」的数字 */
    long pendingInstalls(Long skillId);

    /**
     * 把增量同步到 MySQL（由现有的定时任务一并调用）。
     *
     * @return 本次同步的 Skill 条数
     */
    int flushInstallsToDatabase();
}
