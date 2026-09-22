package com.contenthub.web.service.impl;

import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.domain.dos.FavoriteDO;
import com.contenthub.common.domain.mapper.ContentCategoryMapper;
import com.contenthub.common.domain.mapper.ContentMapper;
import com.contenthub.common.domain.mapper.FavoriteMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.jwt.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplTest {
    @Mock FavoriteMapper favoriteMapper;
    @Mock ContentMapper contentMapper;
    @Mock ContentCategoryMapper categoryMapper;
    @InjectMocks FavoriteServiceImpl service;

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test
    void anonymousCannotAddOrRemoveFavorites() {
        assertCode(ResponseCodeEnum.UNAUTHORIZED, () -> service.favorite(1L));
        assertCode(ResponseCodeEnum.UNAUTHORIZED, () -> service.unfavorite(1L));
        verifyNoInteractions(favoriteMapper, contentMapper, categoryMapper);
    }

    @Test
    void missingContentCannotBeFavorited() {
        login();
        assertCode(ResponseCodeEnum.CONTENT_NOT_FOUND, () -> service.favorite(404L));
        verifyNoInteractions(favoriteMapper);
    }

    @Test
    void duplicateFavoriteIsRejectedBeforeInsert() {
        login();
        when(contentMapper.selectById(1L)).thenReturn(content());
        when(favoriteMapper.selectCount(any())).thenReturn(1L);
        assertCode(ResponseCodeEnum.ALREADY_FAVORITED, () -> service.favorite(1L));
        verify(favoriteMapper, never()).insert(any(FavoriteDO.class));
    }

    @Test
    void concurrentUniqueConstraintViolationUsesSameBusinessError() {
        login();
        when(contentMapper.selectById(1L)).thenReturn(content());
        when(favoriteMapper.selectCount(any())).thenReturn(0L);
        when(favoriteMapper.insert(any(FavoriteDO.class))).thenThrow(new DuplicateKeyException("uk_user_content"));
        assertCode(ResponseCodeEnum.ALREADY_FAVORITED, () -> service.favorite(1L));
        verify(favoriteMapper).insert(argThat((FavoriteDO favorite) ->
                favorite.getUserId().equals(20L) && favorite.getContentId().equals(1L)));
    }

    @Test
    void unfavoriteMissingRecordDoesNotDeleteAnything() {
        login();
        assertCode(ResponseCodeEnum.NOT_FAVORITED, () -> service.unfavorite(1L));
        verify(favoriteMapper, never()).deleteById(anyLong());
    }

    @Test
    void removingFavoritePermitsANewInsertForSameUserAndContent() {
        login();
        when(favoriteMapper.selectOne(any())).thenReturn(
                FavoriteDO.builder().id(99L).userId(20L).contentId(1L).build());
        when(contentMapper.selectById(1L)).thenReturn(content());
        when(favoriteMapper.selectCount(any())).thenReturn(0L);
        assertDoesNotThrow(() -> service.unfavorite(1L));
        assertDoesNotThrow(() -> service.favorite(1L));
        var ordered = inOrder(favoriteMapper);
        ordered.verify(favoriteMapper).selectOne(any());
        ordered.verify(favoriteMapper).deleteById(99L);
        ordered.verify(favoriteMapper).selectCount(any());
        ordered.verify(favoriteMapper).insert(argThat((FavoriteDO favorite) ->
                favorite.getUserId().equals(20L) && favorite.getContentId().equals(1L)));
    }

    @Test
    void absentUserOrContentNeverQueriesFavoriteExistence() {
        assertFalse(service.isFavorited(null, 1L));
        assertFalse(service.isFavorited(20L, null));
        verifyNoInteractions(favoriteMapper);
    }

    private ContentDO content() {
        return ContentDO.builder().id(1L).creatorId(10L).status("PUBLISHED").accessType("FREE").build();
    }

    private void login() {
        LoginUser user = new LoginUser(20L, "reader", "unused", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private void assertCode(ResponseCodeEnum expected, org.junit.jupiter.api.function.Executable action) {
        assertEquals(expected.getErrorCode(), assertThrows(BizException.class, action).getErrorCode());
    }
}
