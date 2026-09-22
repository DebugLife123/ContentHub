package com.contenthub.common.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contenthub.common.domain.dos.SkillCategoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SkillCategoryMapper extends BaseMapper<SkillCategoryDO> {

    /**
     * 按名称查询，<b>不过滤逻辑删除</b>。
     *
     * <p>原因与 {@code ContentCategoryMapper} 相同：{@code uk_skill_category_name}
     * 是唯一索引，而逻辑删除只是把 {@code is_deleted} 置 1、行仍占着唯一键，
     * 所以「删掉分类后用同名再建」会撞唯一键。</p>
     */
    @Select("SELECT * FROM skill_category WHERE name = #{name} LIMIT 1")
    SkillCategoryDO findByNameIncludingDeleted(@Param("name") String name);

    /** 复活一条被逻辑删除的分类；必须走原生 SQL，否则会被自动追加 is_deleted = 0 */
    @Select("UPDATE skill_category SET is_deleted = 0, sort = #{sort}, status = #{status} WHERE id = #{id}")
    void revive(@Param("id") Long id, @Param("sort") Integer sort, @Param("status") String status);
}
