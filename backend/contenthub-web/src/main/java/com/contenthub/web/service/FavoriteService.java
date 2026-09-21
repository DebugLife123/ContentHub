package com.contenthub.web.service;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.ContentListVO;

public interface FavoriteService {

    /** 收藏（计划 Day 28：防重复收藏） */
    Response<Void> favorite(Long contentId);

    /** 取消收藏 */
    Response<Void> unfavorite(Long contentId);

    /** 我的收藏（个人中心用） */
    Response<PageResponse<ContentListVO>> myFavorites(long pageNum, long pageSize);

    /** 该用户是否已收藏该内容 */
    boolean isFavorited(Long userId, Long contentId);

    /** 内容的收藏数 */
    long countByContent(Long contentId);
}
