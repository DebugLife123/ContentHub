package com.contenthub.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页返回结构。
 *
 * <p>计划 §10 阶段 1 要求「内容 CRUD + 分页查询」，阶段 7 要求「统一分页与返回格式」，
 * 这里提前定好，避免后续各接口各写一套分页字段。</p>
 *
 * @param <T> 列表元素类型（通常是 VO，而不是 DO）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> implements Serializable {

    /** 当前页数据 */
    private List<T> list;

    /** 总记录数（来自数据库 count，不是内存里的 list.size()） */
    private long total;

    /** 当前页码，从 1 开始 */
    private long pageNum;

    /** 每页条数 */
    private long pageSize;

    /** 总页数 */
    private long pages;

    /**
     * 由 MyBatis-Plus 的 IPage 直接转换
     */
    public static <T> PageResponse<T> of(IPage<T> page) {
        return PageResponse.<T>builder()
                .list(page.getRecords())
                .total(page.getTotal())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .pages(page.getPages())
                .build();
    }

    /**
     * 由 MyBatis-Plus 的 IPage 转换，并把 DO 映射为 VO
     */
    public static <E, T> PageResponse<T> of(IPage<E> page, Function<E, T> mapper) {
        List<T> mapped = page.getRecords().stream().map(mapper).toList();
        return PageResponse.<T>builder()
                .list(mapped)
                .total(page.getTotal())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .pages(page.getPages())
                .build();
    }
}
