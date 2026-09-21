package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.contenthub.common.domain.dos.ContentCategoryDO;
import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.domain.dos.FavoriteDO;
import com.contenthub.common.domain.mapper.ContentCategoryMapper;
import com.contenthub.common.domain.mapper.ContentMapper;
import com.contenthub.common.domain.mapper.FavoriteMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.vo.ContentListVO;
import com.contenthub.web.service.FavoriteService;
import com.contenthub.web.util.CurrentUserUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ContentMapper contentMapper;
    private final ContentCategoryMapper categoryMapper;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper,
                              ContentMapper contentMapper,
                              ContentCategoryMapper categoryMapper) {
        this.favoriteMapper = favoriteMapper;
        this.contentMapper = contentMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional
    public Response<Void> favorite(Long contentId) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        ContentDO content = contentMapper.selectById(contentId);
        if (Objects.isNull(content)) {
            throw new BizException(ResponseCodeEnum.CONTENT_NOT_FOUND);
        }

        if (isFavorited(loginUser.getUserId(), contentId)) {
            throw new BizException(ResponseCodeEnum.ALREADY_FAVORITED);
        }

        try {
            favoriteMapper.insert(FavoriteDO.builder()
                    .userId(loginUser.getUserId())
                    .contentId(contentId)
                    .build());
        } catch (DuplicateKeyException e) {
            // 并发下两个请求同时通过上面的存在性检查时，靠 uk_user_content 兜底
            throw new BizException(ResponseCodeEnum.ALREADY_FAVORITED);
        }

        return Response.success();
    }

    @Override
    @Transactional
    public Response<Void> unfavorite(Long contentId) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        FavoriteDO favorite = favoriteMapper.selectOne(new LambdaQueryWrapper<FavoriteDO>()
                .eq(FavoriteDO::getUserId, loginUser.getUserId())
                .eq(FavoriteDO::getContentId, contentId));
        if (Objects.isNull(favorite)) {
            throw new BizException(ResponseCodeEnum.NOT_FAVORITED);
        }

        // FavoriteDO 没有 @TableLogic，这里是物理删除：
        // 逻辑删除会让已删除行继续占用 uk_user_content，导致无法重新收藏
        favoriteMapper.deleteById(favorite.getId());
        return Response.success();
    }

    @Override
    public Response<PageResponse<ContentListVO>> myFavorites(long pageNum, long pageSize) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        Page<FavoriteDO> page = favoriteMapper.selectPage(
                Page.of(pageNum, pageSize),
                new LambdaQueryWrapper<FavoriteDO>()
                        .eq(FavoriteDO::getUserId, loginUser.getUserId())
                        .orderByDesc(FavoriteDO::getId));

        if (page.getRecords().isEmpty()) {
            return Response.success(PageResponse.<ContentListVO>builder()
                    .list(List.of())
                    .total(page.getTotal())
                    .pageNum(page.getCurrent())
                    .pageSize(page.getSize())
                    .pages(page.getPages())
                    .build());
        }

        List<Long> contentIds = page.getRecords().stream().map(FavoriteDO::getContentId).toList();
        Map<Long, ContentDO> contents = contentMapper.selectBatchIds(contentIds).stream()
                .collect(Collectors.toMap(ContentDO::getId, c -> c, (a, b) -> a));

        Map<Long, String> categoryNames = categoryNames(contents.values());

        List<ContentListVO> list = contentIds.stream()
                .map(contents::get)
                .filter(Objects::nonNull)
                .map(c -> toVO(c, categoryNames.get(c.getCategoryId())))
                .toList();

        return Response.success(PageResponse.<ContentListVO>builder()
                .list(list)
                .total(page.getTotal())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .pages(page.getPages())
                .build());
    }

    @Override
    public boolean isFavorited(Long userId, Long contentId) {
        if (userId == null || contentId == null) {
            return false;
        }
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<FavoriteDO>()
                .eq(FavoriteDO::getUserId, userId)
                .eq(FavoriteDO::getContentId, contentId));
        return count != null && count > 0;
    }

    @Override
    public long countByContent(Long contentId) {
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<FavoriteDO>()
                .eq(FavoriteDO::getContentId, contentId));
        return count == null ? 0L : count;
    }

    private Map<Long, String> categoryNames(java.util.Collection<ContentDO> contents) {
        List<Long> ids = contents.stream().map(ContentDO::getCategoryId)
                .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ContentCategoryDO::getId, ContentCategoryDO::getName, (a, b) -> a));
    }

    private ContentListVO toVO(ContentDO c, String categoryName) {
        return ContentListVO.builder()
                .id(c.getId())
                .creatorId(c.getCreatorId())
                .categoryId(c.getCategoryId())
                .categoryName(categoryName)
                .title(c.getTitle())
                .summary(c.getSummary())
                .cover(c.getCover())
                .contentType(c.getContentType())
                .accessType(c.getAccessType())
                .status(c.getStatus())
                .viewCount(c.getViewCount())
                .likeCount(c.getLikeCount())
                .createTime(c.getCreateTime())
                .updateTime(c.getUpdateTime())
                .build();
    }
}
