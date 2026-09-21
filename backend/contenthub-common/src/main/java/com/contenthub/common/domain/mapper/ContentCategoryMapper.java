package com.contenthub.common.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contenthub.common.domain.dos.ContentCategoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ContentCategoryMapper extends BaseMapper<ContentCategoryDO> {

    /**
     * 按名称查询，<b>不过滤逻辑删除</b>。
     *
     * <p>存在的意义：{@code content_category} 上有唯一索引 {@code uk_name}，
     * 而 MyBatis-Plus 的逻辑删除只是把 {@code is_deleted} 置 1、行仍然占着那个唯一键。
     * 因此「删除分类 → 用同名再建一个」会在 INSERT 时撞唯一键。
     * 建分类前先用这个方法查一次（含已删除行），命中已删除行就复活它，
     * 而不是无脑 INSERT。</p>
     */
    @Select("SELECT * FROM content_category WHERE name = #{name} LIMIT 1")
    ContentCategoryDO findByNameIncludingDeleted(@Param("name") String name);

    /**
     * 复活一条被逻辑删除的分类。
     *
     * <p>必须走原生 SQL：MyBatis-Plus 的 update 会自动带上 {@code is_deleted = 0} 条件，
     * 对已删除行不生效。</p>
     */
    @Select("UPDATE content_category SET is_deleted = 0, sort = #{sort}, status = #{status} WHERE id = #{id}")
    void revive(@Param("id") Long id, @Param("sort") Integer sort, @Param("status") String status);
}
