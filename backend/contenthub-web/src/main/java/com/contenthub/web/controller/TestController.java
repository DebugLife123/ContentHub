package com.contenthub.web.controller;

import com.contenthub.common.utils.JsonUtil;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.TestUser;
import com.contenthub.common.aspect.ApiOperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Slf4j
@Tag(name = "首页模块")
public class TestController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/admin/test")
    @ApiOperationLog(description = "测试接口")
    @Operation(summary = "测试接口")
    public Response test(@RequestBody @Validated TestUser testUser) {
        // 打印入参
        log.info(JsonUtil.toJsonString(testUser));

        // 设置三种日期字段值
        testUser.setCreateTime(LocalDateTime.now());
        testUser.setUpdateDate(LocalDate.now());
        testUser.setTime(LocalTime.now());

        return Response.success(testUser);
    }

    /**
     * Redis 连通性验证接口（阶段 0 验收用）。
     *
     * <p>写一个带过期时间的 key 再读回来，确认后端确实连上了 Redis。
     * 验收完成后可以删除本接口。</p>
     */
    @GetMapping("/admin/redis/verify")
    @Operation(summary = "Redis 连通性验证（阶段 0 验收用）")
    public Response redisVerify() {
        String key = "contenthub:stage0:ping";
        String value = "pong@" + LocalDateTime.now();
        Duration ttl = Duration.ofMinutes(10);

        stringRedisTemplate.opsForValue().set(key, value, ttl);
        String readBack = stringRedisTemplate.opsForValue().get(key);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("writtenValue", value);
        result.put("readBackValue", readBack);
        result.put("matched", value.equals(readBack));
        result.put("ttlSeconds", stringRedisTemplate.getExpire(key));
        return Response.success(result);
    }

}
