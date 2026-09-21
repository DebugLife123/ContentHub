package com.contenthub.common.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contenthub.common.domain.dos.SubscriptionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubscriptionMapper extends BaseMapper<SubscriptionDO> {
}