package com.contenthub.common.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contenthub.common.domain.dos.ContentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ContentMapper extends BaseMapper<ContentDO> {

    /**
     * 把驳回原因置空。
     *
     * <p>必须走原生 SQL：MyBatis-Plus 的 {@code updateById} 默认忽略 null 字段，
     * 用实体传 null 是删不掉旧值的。</p>
     */
    @Update("UPDATE contents SET reject_reason = NULL WHERE id = #{id}")
    void clearRejectReason(@Param("id") Long id);
}
