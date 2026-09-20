package com.contenthub.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置。
 *
 * <p>默认的 {@code RedisTemplate} 使用 JDK 序列化，key/value 在 redis-cli 里都是乱码，
 * 这里显式指定：key 用 String，value 用 JSON，便于用 {@code redis-cli} 直接观察数据。</p>
 *
 * <p>连接参数在 {@code application-dev.yml} 的 {@code spring.data.redis} 下配置。</p>
 */
@Configuration
public class RedisConfig {

    /**
     * 通用 RedisTemplate：key 为 String，value 为 JSON。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(buildObjectMapper());

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 当 value 明确只是字符串时（例如阶段 2 存登录 token），用 StringRedisTemplate 更省事。
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    /**
     * 给 JSON 序列化器准备 ObjectMapper：支持 Java 8 时间类型，并写入类型信息以便反序列化回原对象。
     */
    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 写入 @class 类型信息；LaissezFaireSubTypeValidator 不做白名单校验，仅适合本地/学习环境
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }
}
