package com.contenthub.web.service.impl;

import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.common.domain.mapper.ContentCategoryMapper;
import com.contenthub.common.domain.mapper.ContentMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.req.ContentReqVO;
import com.contenthub.web.model.vo.ContentDetailVO;
import com.contenthub.web.service.CommentService;
import com.contenthub.web.service.ContentAccessService;
import com.contenthub.web.service.ContentCacheService;
import com.contenthub.web.service.ContentStatService;
import com.contenthub.web.service.FavoriteService;
import com.contenthub.web.service.NotificationService;
import com.contenthub.web.service.ReadingHistoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceImplTest {
    private static final Long CONTENT_ID = 7L;
    private static final Long CREATOR_ID = 1L;

    @Mock ContentMapper contentMapper;
    @Mock ContentCategoryMapper categoryMapper;
    @Mock ContentAccessService accessService;
    @Mock FavoriteService favoriteService;
    @Mock CommentService commentService;
    @Mock ContentCacheService cacheService;
    @Mock ContentStatService statService;
    @Mock ReadingHistoryService readingHistoryService;
    @Mock NotificationService notificationService;
    @InjectMocks ContentServiceImpl service;

    @BeforeEach
    void startWithEmptySecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"CREATOR", "ADMIN"})
    void createAlwaysInsertsDraftForCurrentCreatorOrAdmin(String role) {
        authenticate(CREATOR_ID, role);
        doAnswer(invocation -> {
            invocation.getArgument(0, ContentDO.class).setId(CONTENT_ID);
            return 1;
        }).when(contentMapper).insert(any(ContentDO.class));

        assertEquals(CONTENT_ID, service.create(request()).getData());

        ContentDO inserted = captureUpdateOrInsert(true);
        assertAll(
                () -> assertEquals(CREATOR_ID, inserted.getCreatorId()),
                () -> assertEquals("DRAFT", inserted.getStatus()),
                () -> assertEquals("Edited", inserted.getTitle()),
                () -> assertEquals("ARTICLE", inserted.getContentType()),
                () -> assertEquals("FREE", inserted.getAccessType()),
                () -> assertEquals(0, inserted.getViewCount()),
                () -> assertEquals(0, inserted.getLikeCount()));
    }

    @Test
    void createRequiresAuthentication() {
        assertCode(ResponseCodeEnum.UNAUTHORIZED, () -> service.create(request()));
        verifyNoInteractions(contentMapper, categoryMapper);
    }

    @Test
    void ordinaryReaderCannotCreateContent() {
        authenticate(2L, "USER");
        assertCode(ResponseCodeEnum.NOT_CREATOR, () -> service.create(request()));
        verifyNoInteractions(contentMapper, categoryMapper);
    }

    @Test
    void createRejectsUnknownCategoryWithoutInserting() {
        authenticate(CREATOR_ID, "CREATOR");
        ContentReqVO req = request();
        req.setCategoryId(99L);

        assertCode(ResponseCodeEnum.CATEGORY_NOT_FOUND, () -> service.create(req));

        verify(categoryMapper).selectById(99L);
        verifyNoInteractions(contentMapper);
    }

    @ParameterizedTest
    @CsvSource({"DRAFT, 1, CREATOR", "REJECTED, 1, CREATOR", "OFFLINE, 1, CREATOR", "DRAFT, 9, ADMIN"})
    void editableContentCanBeUpdatedWithoutChangingStatusOrOwner(String status, long userId, String role) {
        authenticate(userId, role);
        when(contentMapper.selectById(CONTENT_ID)).thenReturn(content(status));

        assertTrue(service.update(CONTENT_ID, request()).isSuccess());

        ContentDO update = captureUpdateOrInsert(false);
        assertAll(
                () -> assertEquals(CONTENT_ID, update.getId()),
                () -> assertEquals("Edited", update.getTitle()),
                () -> assertEquals("ARTICLE", update.getContentType()),
                () -> assertEquals("Body", update.getBody()),
                () -> assertEquals("SUBSCRIBED", update.getAccessType()),
                () -> assertNull(update.getStatus(), "Editing must not bypass the status transition API"),
                () -> assertNull(update.getCreatorId(), "Editing must not transfer ownership"));
        verify(cacheService).evict(CONTENT_ID);
    }

    @ParameterizedTest
    @CsvSource({"PENDING, 1, CREATOR", "PUBLISHED, 1, CREATOR", "PENDING, 9, ADMIN", "PUBLISHED, 9, ADMIN"})
    void pendingAndPublishedContentCannotBeEditedEvenByAdmin(String status, long userId, String role) {
        authenticate(userId, role);
        when(contentMapper.selectById(CONTENT_ID)).thenReturn(content(status));

        assertCode(ResponseCodeEnum.CONTENT_STATUS_ILLEGAL, () -> service.update(CONTENT_ID, request()));

        verify(contentMapper, never()).updateById(any(ContentDO.class));
        verifyNoInteractions(cacheService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "delete", "submit", "offline", "findMine"})
    void nonOwnerCannotReadOrMutateAnotherCreatorsContent(String operation) {
        authenticate(2L, "CREATOR");
        when(contentMapper.selectById(CONTENT_ID)).thenReturn(content("DRAFT"));

        assertCode(ResponseCodeEnum.NOT_CONTENT_OWNER, () -> perform(operation));

        verify(contentMapper, never()).updateById(any(ContentDO.class));
        verify(contentMapper, never()).deleteById(CONTENT_ID);
        verify(cacheService, never()).evict(anyLong());
        verifyNoInteractions(notificationService, accessService, categoryMapper);
    }

    @Test
    void editingRequiresAuthentication() {
        when(contentMapper.selectById(CONTENT_ID)).thenReturn(content("DRAFT"));
        assertCode(ResponseCodeEnum.UNAUTHORIZED, () -> service.update(CONTENT_ID, request()));
        verify(contentMapper, never()).updateById(any(ContentDO.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "delete", "submit", "offline", "approve", "reject", "findMine"})
    void missingContentIsReportedBeforeAnyMutation(String operation) {
        authenticate(CREATOR_ID, "CREATOR");

        assertCode(ResponseCodeEnum.CONTENT_NOT_FOUND, () -> perform(operation));

        verify(contentMapper, never()).updateById(any(ContentDO.class));
        verify(contentMapper, never()).deleteById(CONTENT_ID);
        verifyNoInteractions(notificationService);
        verify(cacheService, never()).evict(anyLong());
    }

    @ParameterizedTest
    @CsvSource({"1, CREATOR", "9, ADMIN"})
    void ownerAndAdministratorCanDeleteContent(long userId, String role) {
        authenticate(userId, role);
        when(contentMapper.selectById(CONTENT_ID)).thenReturn(content("DRAFT"));

        assertTrue(service.delete(CONTENT_ID).isSuccess());

        verify(contentMapper).deleteById(CONTENT_ID);
        verify(cacheService).evict(CONTENT_ID);
    }

    @ParameterizedTest
    @CsvSource({"DRAFT, submit, PENDING", "REJECTED, submit, PENDING", "OFFLINE, submit, PENDING",
            "PUBLISHED, offline, OFFLINE", "PENDING, approve, PUBLISHED", "PENDING, reject, REJECTED"})
    void allowedTransitionsUpdateStatusInvalidateCacheAndHandleRejectionReason(
            String current, String operation, String target) {
        authenticate(CREATOR_ID, operation.equals("approve") || operation.equals("reject") ? "ADMIN" : "CREATOR");
        when(contentMapper.selectById(CONTENT_ID)).thenReturn(content(current));
        // 条件更新命中 1 行 = 流转成功
        when(contentMapper.transitionStatus(eq(CONTENT_ID), eq(current), eq(target), any()))
                .thenReturn(1);

        perform(operation);

        if ("REJECTED".equals(target)) {
            verify(contentMapper).transitionStatus(CONTENT_ID, current, target, "Needs revision");
            verify(notificationService).push(eq(CREATOR_ID), eq("CONTENT_REJECTED"), anyString(),
                    contains("Needs revision"), eq("CONTENT"), eq(CONTENT_ID));
        } else {
            // 非驳回时必须把 rejectReason 传 null：同一条 UPDATE 顺带清空上次的驳回原因
            verify(contentMapper).transitionStatus(CONTENT_ID, current, target, null);
            if ("PUBLISHED".equals(target)) {
                verify(notificationService).push(eq(CREATOR_ID), eq("CONTENT_APPROVED"), anyString(),
                        contains("Original title"), eq("CONTENT"), eq(CONTENT_ID));
            } else {
                verifyNoInteractions(notificationService);
            }
        }
        verify(cacheService).evict(CONTENT_ID);
    }

    /**
     * 并发审核的核心保护：条件更新影响 0 行说明状态已被别人改走，
     * 必须报错且不发通知，否则作者会同时收到「已发布」和「已驳回」。
     */
    @ParameterizedTest
    @CsvSource({"PENDING, approve, PUBLISHED", "PENDING, reject, REJECTED"})
    void concurrentReviewLosesRaceAndMustNotNotifyAuthor(String current, String operation, String target) {
        authenticate(9L, "ADMIN");
        when(contentMapper.selectById(CONTENT_ID)).thenReturn(content(current));
        when(contentMapper.transitionStatus(eq(CONTENT_ID), eq(current), eq(target), any())).thenReturn(0);

        assertCode(ResponseCodeEnum.CONTENT_STATUS_ILLEGAL, () -> perform(operation));

        verifyNoInteractions(notificationService);
        verify(cacheService, never()).evict(anyLong());
    }

    @ParameterizedTest
    @CsvSource({"PENDING, submit", "PUBLISHED, submit", "DRAFT, offline", "PENDING, offline",
            "REJECTED, offline", "OFFLINE, offline", "DRAFT, approve", "PUBLISHED, approve",
            "REJECTED, approve", "OFFLINE, approve", "DRAFT, reject", "PUBLISHED, reject",
            "REJECTED, reject", "OFFLINE, reject"})
    void illegalTransitionsNeverWriteOrNotify(String current, String operation) {
        authenticate(CREATOR_ID, "ADMIN");
        when(contentMapper.selectById(CONTENT_ID)).thenReturn(content(current));

        assertCode(ResponseCodeEnum.CONTENT_STATUS_ILLEGAL, () -> perform(operation));

        verify(contentMapper, never()).updateById(any(ContentDO.class));
        verify(contentMapper, never()).transitionStatus(anyLong(), anyString(), anyString(), any());
        verifyNoInteractions(cacheService, notificationService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "PENDING", "REJECTED", "OFFLINE"})
    void publicDetailNeverExposesUnpublishedContent(String status) {
        when(cacheService.get(CONTENT_ID)).thenReturn(content(status));

        assertCode(ResponseCodeEnum.CONTENT_NOT_FOUND, () -> service.findPublishedById(CONTENT_ID));

        verifyNoInteractions(accessService, statService, readingHistoryService);
    }

    @Test
    void lockedPublicDetailHidesBodyAndAttachmentAndReturnsOnlyPreview() {
        ContentDO content = content("PUBLISHED");
        content.setBody("x".repeat(ContentAccessService.PREVIEW_LENGTH + 30));
        when(cacheService.get(CONTENT_ID)).thenReturn(content);
        when(accessService.decide(content, null)).thenReturn(ContentAccessService.AccessDecision.deny("Subscribe first"));

        ContentDetailVO detail = service.findPublishedById(CONTENT_ID).getData();

        assertTrue(detail.getLocked());
        assertNull(detail.getBody());
        assertNull(detail.getFileUrl());
        assertEquals("x".repeat(ContentAccessService.PREVIEW_LENGTH) + "……", detail.getBodyPreview());
        assertEquals("Subscribe first", detail.getLockReason());
        verify(statService).recordView(CONTENT_ID);
        verify(readingHistoryService).record(CONTENT_ID, null);
        verify(contentMapper, never()).selectById(anyLong());
    }

    @Test
    void permittedPublicDetailReturnsFullContentAndPopulatesCacheOnMiss() {
        ContentDO content = content("PUBLISHED");
        when(contentMapper.selectById(CONTENT_ID)).thenReturn(content);
        when(accessService.decide(content, null)).thenReturn(ContentAccessService.AccessDecision.allow());

        ContentDetailVO detail = service.findPublishedById(CONTENT_ID).getData();

        assertFalse(detail.getLocked());
        assertEquals(content.getBody(), detail.getBody());
        assertEquals(content.getFileUrl(), detail.getFileUrl());
        assertNull(detail.getBodyPreview());
        assertNull(detail.getLockReason());
        verify(cacheService).put(content);
    }

    @Test
    void ownerCanReadFullDraftWithoutSubscriptionDecision() {
        authenticate(CREATOR_ID, "CREATOR");
        ContentDO content = content("DRAFT");
        when(contentMapper.selectById(CONTENT_ID)).thenReturn(content);

        ContentDetailVO detail = service.findMineById(CONTENT_ID).getData();

        assertFalse(detail.getLocked());
        assertEquals(content.getBody(), detail.getBody());
        assertEquals(content.getFileUrl(), detail.getFileUrl());
        verifyNoInteractions(accessService);
    }

    private void perform(String operation) {
        switch (operation) {
            case "update" -> service.update(CONTENT_ID, request());
            case "delete" -> service.delete(CONTENT_ID);
            case "submit" -> service.submit(CONTENT_ID);
            case "offline" -> service.offline(CONTENT_ID);
            case "approve" -> service.approve(CONTENT_ID);
            case "reject" -> service.reject(CONTENT_ID, "Needs revision");
            case "findMine" -> service.findMineById(CONTENT_ID);
            default -> throw new IllegalArgumentException(operation);
        }
    }

    private ContentDO captureUpdateOrInsert(boolean insert) {
        ArgumentCaptor<ContentDO> captor = ArgumentCaptor.forClass(ContentDO.class);
        if (insert) {
            verify(contentMapper).insert(captor.capture());
        } else {
            verify(contentMapper).updateById(captor.capture());
        }
        return captor.getValue();
    }

    private void assertCode(ResponseCodeEnum expected, Executable action) {
        assertEquals(expected.getErrorCode(), assertThrows(BizException.class, action).getErrorCode());
    }

    private void authenticate(long id, String role) {
        LoginUser user = new LoginUser(id, "user-" + id, "unused", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private ContentDO content(String status) {
        return ContentDO.builder().id(CONTENT_ID).creatorId(CREATOR_ID).status(status)
                .title("Original title").body("Original body").fileUrl("/private/file.pdf")
                .accessType("SUBSCRIBED").rejectReason("Old rejection").build();
    }

    private ContentReqVO request() {
        ContentReqVO request = new ContentReqVO();
        request.setTitle("  Edited  ");
        request.setContentType(" article ");
        request.setBody("Body");
        return request;
    }
}
