package com.contenthub.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author newone
 * @date 2023/11/14
 * @description Json 工具类
 */
@Slf4j
public class JsonUtil {
    private static final ObjectMapper INSTANCE = new ObjectMapper();

    public static String toJsonString(Object obj){
        try{
            return INSTANCE.writeValueAsString(obj);
        } catch (JsonProcessingException e){
            return obj.toString();
        }
    }

    /**
     * 把 JSON 字符串反序列化成列表。
     *
     * <p>用于读取 {@code skill.quick_start} 这类存在文本列里的结构化数据。
     * 解析失败不抛异常——库里存了脏数据时，页面应该少显示一块，而不是整个接口 500。</p>
     */
    public static <T> List<T> parseList(String json, Class<T> elementType) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return INSTANCE.readValue(
                    json,
                    INSTANCE.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (JsonProcessingException e) {
            log.warn("JSON 解析失败，按空列表处理: {}", json, e);
            return List.of();
        }
    }
}
