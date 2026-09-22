package com.contenthub.common.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contenthub.common.domain.dos.SubscriptionDO;
import com.contenthub.common.domain.dos.UserDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SubscriptionMapper extends BaseMapper<SubscriptionDO> {
    @Select("SELECT id, username, password, nickname, email, role, status, create_time, update_time, is_deleted FROM users WHERE id = #{userId} FOR UPDATE")
    UserDO lockUser(@Param("userId") Long userId);
}