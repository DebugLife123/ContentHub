package com.contenthub.web.model.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询的公共父类。
 *
 * <p>抽出来是因为踩过一次：内容分页有 {@code @Min/@Max} 校验，后加的 Skill 分页
 * 忘了写，于是 {@code pageSize=999999999}、{@code pageNum=-1} 都能拿到 200。
 * 分页参数是所有列表接口共用的，校验写在一处才不会各写各的。</p>
 *
 * <p>分页参数由 Controller 上的 {@code @Validated} 触发校验，非法值会被
 * {@code GlobalExceptionHandler} 统一转成 400 + 统一响应体。</p>
 */
@Data
public class PageReqVO implements Serializable {

    /** 每页条数上限，防止一次把整张表捞出来 */
    public static final long MAX_PAGE_SIZE = 100;

    @Min(value = 1, message = "页码从 1 开始")
    private Long pageNum = 1L;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = MAX_PAGE_SIZE, message = "每页最多 " + MAX_PAGE_SIZE + " 条")
    private Long pageSize = 10L;
}
