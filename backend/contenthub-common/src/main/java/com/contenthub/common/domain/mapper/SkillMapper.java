package com.contenthub.common.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contenthub.common.domain.dos.SkillDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SkillMapper extends BaseMapper<SkillDO> {

    /**
     * 按名称查询，<b>不过滤逻辑删除</b>。
     *
     * <p>{@code uk_skill_name} 是唯一索引，逻辑删除的行仍占着唯一键，
     * 因此「删除 Skill → 用同名再建」会撞唯一键。</p>
     */
    @Select("SELECT * FROM skill WHERE name = #{name} LIMIT 1")
    SkillDO findByNameIncludingDeleted(@Param("name") String name);

    /** 复活一条被逻辑删除的 Skill；状态回到草稿，避免直接复活就对外可见 */
    @Select("UPDATE skill SET is_deleted = 0, status = 'DRAFT' WHERE id = #{id}")
    void revive(@Param("id") Long id);
}
