package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.contenthub.common.constants.RedisKeys;
import com.contenthub.common.domain.dos.SkillDO;
import com.contenthub.common.domain.mapper.SkillMapper;
import com.contenthub.web.service.SkillStatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
public class SkillStatServiceImpl implements SkillStatService {

    private final StringRedisTemplate redis;
    private final SkillMapper skillMapper;

    public SkillStatServiceImpl(StringRedisTemplate redis, SkillMapper skillMapper) {
        this.redis = redis;
        this.skillMapper = skillMapper;
    }

    @Override
    public void recordInstall(Long skillId) {
        if (skillId == null) {
            return;
        }
        try {
            redis.opsForValue().increment(RedisKeys.skillInstall(skillId));
            redis.opsForSet().add(RedisKeys.SKILL_INSTALL_DIRTY_SET, String.valueOf(skillId));
        } catch (Exception e) {
            // 统计失败不该让「安装」这个动作失败：用户要的是命令，计数是附属品
            log.warn("记录 Skill 安装量失败 skillId={}", skillId, e);
        }
    }

    @Override
    public long pendingInstalls(Long skillId) {
        if (skillId == null) {
            return 0;
        }
        try {
            String value = redis.opsForValue().get(RedisKeys.skillInstall(skillId));
            return value == null ? 0 : Long.parseLong(value);
        } catch (Exception e) {
            log.warn("读取 Skill 待同步安装量失败 skillId={}", skillId, e);
            return 0;
        }
    }

    @Override
    public int flushInstallsToDatabase() {
        Set<String> dirty = redis.opsForSet().members(RedisKeys.SKILL_INSTALL_DIRTY_SET);
        if (dirty == null || dirty.isEmpty()) {
            return 0;
        }

        int synced = 0;
        for (String idText : dirty) {
            long delta = takeDelta(idText);
            if (delta > 0) {
                // 用 downloads = downloads + N 而不是「先读再写」：后者在管理员同时编辑
                // 这个 Skill 时会丢掉一次更新。这里的 delta 是 Java long，字符串形态只可能是数字，
                // 不存在拼接注入。
                skillMapper.update(null, new LambdaUpdateWrapper<SkillDO>()
                        .eq(SkillDO::getId, Long.valueOf(idText))
                        .setSql("downloads = downloads + " + delta));
                synced++;
            }
            // 先删计数再移出 dirty：反过来会在两步之间丢增量
            redis.delete(RedisKeys.skillInstall(idText));
            redis.opsForSet().remove(RedisKeys.SKILL_INSTALL_DIRTY_SET, idText);
        }

        if (synced > 0) {
            log.info("同步 Skill 安装量到数据库，共 {} 条", synced);
        }
        return synced;
    }

    /** 读取并校验增量；脏数据按 0 处理，不抛出去影响其它条目 */
    private long takeDelta(String idText) {
        try {
            String value = redis.opsForValue().get(RedisKeys.skillInstall(idText));
            return value == null ? 0 : Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Skill 安装量不是合法数字，按 0 处理：{}", idText);
            return 0;
        }
    }
}
