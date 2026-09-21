package com.contenthub.common.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.contenthub.common.domain.dos.FavoriteDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<FavoriteDO> {
}