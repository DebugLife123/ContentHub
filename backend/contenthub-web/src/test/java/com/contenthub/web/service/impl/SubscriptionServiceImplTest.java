package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.contenthub.common.domain.dos.SubscriptionDO;
import com.contenthub.common.domain.dos.SubscriptionPaymentDO;
import com.contenthub.common.domain.dos.SubscriptionPlanDO;
import com.contenthub.common.domain.mapper.CreatorProfileMapper;
import com.contenthub.common.domain.mapper.SubscriptionMapper;
import com.contenthub.common.domain.mapper.SubscriptionPaymentMapper;
import com.contenthub.common.domain.mapper.SubscriptionPlanMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.vo.SubscriptionVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {
    private static final Long USER_ID = 1L;
    private static final Long CREATOR_ID = 2L;
    private static final Long PLAN_ID = 10L;
    private static final Long SUBSCRIPTION_ID = 20L;

    @Mock SubscriptionMapper subscriptionMapper;
    @Mock SubscriptionPlanMapper planMapper;
    @Mock CreatorProfileMapper creatorProfileMapper;
    /** 支付流水账本：payMock 会先按幂等键查这里，再落一行流水 */
    @Mock SubscriptionPaymentMapper paymentMapper;
    @InjectMocks SubscriptionServiceImpl service;
    @Captor ArgumentCaptor<LambdaQueryWrapper<SubscriptionDO>> queryCaptor;

    @BeforeAll
    static void initializeLambdaColumnMetadata() {
        // No Spring/database is required, but MyBatis needs metadata to render captured lambda queries.
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "subscription-tests"),
                SubscriptionDO.class);
    }

    @BeforeEach
    void startWithEmptySecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void firstPurchaseCreatesActiveSubscriptionForFullPlanDuration() {
        authenticate(USER_ID, "USER");
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan("ACTIVE"));
        doAnswer(invocation -> {
            invocation.getArgument(0, SubscriptionDO.class).setId(SUBSCRIPTION_ID);
            return 1;
        }).when(subscriptionMapper).insert(any(SubscriptionDO.class));
        LocalDateTime before = LocalDateTime.now();

        SubscriptionVO result = service.payMock(PLAN_ID).getData();

        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<SubscriptionDO> captor = ArgumentCaptor.forClass(SubscriptionDO.class);
        verify(subscriptionMapper).insert(captor.capture());
        SubscriptionDO inserted = captor.getValue();
        assertAll(
                () -> assertEquals(USER_ID, inserted.getUserId()),
                () -> assertEquals(CREATOR_ID, inserted.getCreatorId()),
                () -> assertEquals(PLAN_ID, inserted.getPlanId()),
                () -> assertEquals("ACTIVE", inserted.getStatus()),
                () -> assertBetween(inserted.getStartTime(), before, after),
                () -> assertEquals(inserted.getStartTime().plusDays(30), inserted.getEndTime()),
                () -> assertEquals(SUBSCRIPTION_ID, result.getId()),
                () -> assertEquals("Monthly", result.getPlanName()),
                () -> assertEquals(inserted.getEndTime(), result.getEndTime()),
                () -> assertTrue(result.getValid()),
                () -> assertEquals("ACTIVE", result.getStatus()));
        verify(subscriptionMapper, never()).updateById(any(SubscriptionDO.class));
    }

    /**
     * 幂等：同一个 Idempotency-Key 重复提交只生效一次。
     *
     * <p>对应真实场景「请求超时但服务端已经提交，用户又点了一次」——
     * 第二次必须原样返回第一次的结果，不能变成两次购买/续期。</p>
     */
    @Test
    void repeatedIdempotencyKeyReturnsOriginalResultWithoutPurchasingAgain() {
        authenticate(USER_ID, "USER");
        SubscriptionDO existing = subscription("ACTIVE", LocalDateTime.now().plusDays(10));
        SubscriptionPaymentDO prior = SubscriptionPaymentDO.builder()
                .userId(USER_ID)
                .subscriptionId(SUBSCRIPTION_ID)
                .planId(PLAN_ID)
                .creatorId(CREATOR_ID)
                .idempotencyKey("key-1")
                .planNameSnapshot("Monthly")
                .build();
        when(paymentMapper.selectOne(any())).thenReturn(prior);
        when(subscriptionMapper.selectById(SUBSCRIPTION_ID)).thenReturn(existing);

        SubscriptionVO result = service.payMock(PLAN_ID, "key-1").getData();

        assertAll(
                () -> assertEquals(SUBSCRIPTION_ID, result.getId()),
                () -> assertEquals(existing.getEndTime(), result.getEndTime()),
                () -> assertEquals("Monthly", result.getPlanName()));
        // 关键：没有新建订阅、没有续期写回、没有第二条流水
        verify(subscriptionMapper, never()).insert(any(SubscriptionDO.class));
        verify(subscriptionMapper, never()).updateById(any(SubscriptionDO.class));
        verify(paymentMapper, never()).insert(any(SubscriptionPaymentDO.class));
    }

    @Test
    void renewalReusesExistingSubscriptionAndExtendsOriginalExpiryWithoutLosingRemainingTime() {
        authenticate(USER_ID, "USER");
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan("ACTIVE"));
        SubscriptionDO active = subscription("ACTIVE", LocalDateTime.now().plusDays(12));
        active.setPlanId(99L);
        LocalDateTime originalEnd = active.getEndTime();
        LocalDateTime originalStart = active.getStartTime();
        when(subscriptionMapper.selectOne(any())).thenReturn(active);

        SubscriptionVO result = service.payMock(PLAN_ID).getData();

        ArgumentCaptor<SubscriptionDO> captor = ArgumentCaptor.forClass(SubscriptionDO.class);
        verify(subscriptionMapper).updateById(captor.capture());
        SubscriptionDO renewed = captor.getValue();
        assertAll(
                () -> assertEquals(SUBSCRIPTION_ID, renewed.getId()),
                () -> assertEquals(USER_ID, renewed.getUserId()),
                () -> assertEquals(CREATOR_ID, renewed.getCreatorId()),
                () -> assertEquals(PLAN_ID, renewed.getPlanId()),
                () -> assertEquals(originalStart, renewed.getStartTime()),
                () -> assertEquals(originalEnd.plusDays(30), renewed.getEndTime()),
                () -> assertEquals(renewed.getEndTime(), result.getEndTime()),
                () -> assertTrue(result.getValid()));
        verify(subscriptionMapper, never()).insert(any(SubscriptionDO.class));
    }

    @Test
    void purchaseRequiresLoginBeforeLookingUpPlan() {
        assertCode(ResponseCodeEnum.UNAUTHORIZED, () -> service.payMock(PLAN_ID));
        verifyNoInteractions(planMapper, subscriptionMapper, creatorProfileMapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "CREATOR", "ADMIN"})
    void nobodyCanPurchaseTheirOwnPlan(String role) {
        authenticate(CREATOR_ID, role);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan("ACTIVE"));

        assertCode(ResponseCodeEnum.PARAM_NOT_VALID, () -> service.payMock(PLAN_ID));

        // payMock 会先 lockUser 串行化（因此 subscriptionMapper 有交互），
        // 但校验失败时不允许产生任何写入或流水
        assertNoSideEffects();
    }

    @ParameterizedTest
    @ValueSource(strings = {"INACTIVE", "MISSING"})
    void missingOrInactivePlanCannotBePurchased(String status) {
        authenticate(USER_ID, "USER");
        when(planMapper.selectById(PLAN_ID)).thenReturn("MISSING".equals(status) ? null : plan(status));

        assertCode(ResponseCodeEnum.PLAN_NOT_FOUND, () -> service.payMock(PLAN_ID));

        assertNoSideEffects();
    }

    /** 校验失败/被拒绝的路径都不得落库：无订阅写入、无流水、无资料查询 */
    private void assertNoSideEffects() {
        verify(subscriptionMapper, never()).insert(any(SubscriptionDO.class));
        verify(subscriptionMapper, never()).updateById(any(SubscriptionDO.class));
        verify(subscriptionMapper, never()).update(isNull(), any());
        verify(paymentMapper, never()).insert(any(SubscriptionPaymentDO.class));
        verifyNoInteractions(creatorProfileMapper);
    }

    @ParameterizedTest
    @CsvSource({", 2", "1,", ","})
    void nullUserOrCreatorHasNoActiveSubscription(Long userId, Long creatorId) {
        assertFalse(service.hasActiveSubscription(userId, creatorId));
        verifyNoInteractions(subscriptionMapper);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void activeLookupRequiresMatchingUserCreatorActiveStatusAndUnexpiredEndTime(boolean found) {
        when(subscriptionMapper.selectOne(any())).thenReturn(found
                ? subscription("ACTIVE", LocalDateTime.now().plusDays(1)) : null);
        LocalDateTime before = LocalDateTime.now();

        assertEquals(found, service.hasActiveSubscription(USER_ID, CREATOR_ID));

        LocalDateTime after = LocalDateTime.now();
        verify(subscriptionMapper).selectOne(queryCaptor.capture());
        LambdaQueryWrapper<SubscriptionDO> query = queryCaptor.getValue();
        String sql = query.getSqlSegment();
        assertAll(
                () -> assertTrue(sql.contains("user_id ="), sql),
                () -> assertTrue(sql.contains("creator_id ="), sql),
                () -> assertTrue(sql.contains("status ="), sql),
                () -> assertTrue(sql.contains("end_time >"), "Expiry must be strict, not inclusive: " + sql),
                () -> assertTrue(sql.contains("ORDER BY end_time DESC"), sql),
                () -> assertTrue(sql.contains("LIMIT 1"), sql));
        Collection<Object> parameters = query.getParamNameValuePairs().values();
        assertTrue(parameters.contains(USER_ID));
        assertTrue(parameters.contains(CREATOR_ID));
        assertTrue(parameters.contains("ACTIVE"), "Canceled/refunded/expired rows must not grant access");
        LocalDateTime cutoff = parameters.stream().filter(LocalDateTime.class::isInstance)
                .map(LocalDateTime.class::cast).findFirst().orElseThrow();
        assertBetween(cutoff, before, after);
    }

    @Test
    void anyActiveSubscriptionForAnonymousDoesNotQueryDatabase() {
        assertFalse(service.hasAnyActiveSubscription(null));
        verifyNoInteractions(subscriptionMapper);
    }

    @ParameterizedTest
    @CsvSource({"0, false", "1, true", "3, true", ", false"})
    void anyActiveSubscriptionUsesActiveStatusAndStrictExpiry(Long count, boolean expected) {
        when(subscriptionMapper.selectCount(any())).thenReturn(count);

        assertEquals(expected, service.hasAnyActiveSubscription(USER_ID));

        verify(subscriptionMapper).selectCount(queryCaptor.capture());
        LambdaQueryWrapper<SubscriptionDO> query = queryCaptor.getValue();
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("user_id ="), sql);
        assertTrue(sql.contains("status ="), sql);
        assertTrue(sql.contains("end_time >"), sql);
        assertTrue(query.getParamNameValuePairs().containsValue(USER_ID));
        assertTrue(query.getParamNameValuePairs().containsValue("ACTIVE"));
    }

    @ParameterizedTest
    @CsvSource({"cancel, CANCELED", "refund, REFUNDED"})
    void ownerCanCloseActiveSubscriptionAndRecordClosingTime(String operation, String target) {
        authenticate(USER_ID, "USER");
        SubscriptionDO active = subscription("ACTIVE", LocalDateTime.now().plusDays(10));
        when(subscriptionMapper.selectById(SUBSCRIPTION_ID)).thenReturn(active);
        // 关闭走「条件 UPDATE ... WHERE status = ACTIVE」，必须命中 1 行才算成功
        when(subscriptionMapper.update(isNull(), any())).thenReturn(1);
        LocalDateTime before = LocalDateTime.now();

        close(operation);

        LocalDateTime after = LocalDateTime.now();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<SubscriptionDO>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(subscriptionMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<SubscriptionDO> update = captor.getValue();
        Collection<Object> params = update.getParamNameValuePairs().values();
        assertAll(
                // SET 子句在 getSqlSet()，WHERE 条件在 getSqlSegment()
                () -> assertTrue(update.getSqlSet().contains("closed_time"), update.getSqlSet()),
                () -> assertTrue(update.getSqlSegment().contains("status ="), update.getSqlSegment()),
                () -> assertTrue(params.contains(target), "目标状态应写进 UPDATE：" + params),
                () -> assertTrue(params.contains(SUBSCRIPTION_ID), "条件必须锁定这条订阅：" + params),
                // 关闭不改变合同到期时间，只改状态与关闭时间
                () -> assertFalse(params.contains(active.getEndTime()), "不应写入 end_time"));
        verify(subscriptionMapper, never()).updateById(any(SubscriptionDO.class));
        verifyNoInteractions(planMapper, creatorProfileMapper);
    }

    /**
     * 并发保护：refund 与 pay 交错时，payMock 早先读到的 ACTIVE 快照不能把已退款的订阅写回 ACTIVE。
     * 条件更新命中 0 行时必须报错，而不是当作关闭成功。
     */
    @ParameterizedTest
    @CsvSource({"cancel, CANCELED", "refund, REFUNDED"})
    void closeLosesRaceWhenStatusAlreadyChangedAndMustReportFailure(String operation, String target) {
        authenticate(USER_ID, "USER");
        when(subscriptionMapper.selectById(SUBSCRIPTION_ID))
                .thenReturn(subscription("ACTIVE", LocalDateTime.now().plusDays(10)));
        when(subscriptionMapper.update(isNull(), any())).thenReturn(0);

        assertCode(ResponseCodeEnum.SUBSCRIPTION_NOT_ACTIVE, () -> close(operation));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancel", "refund"})
    void closingRequiresAuthentication(String operation) {
        assertCode(ResponseCodeEnum.UNAUTHORIZED, () -> close(operation));
        verifyNoInteractions(subscriptionMapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancel", "refund"})
    void closingMissingSubscriptionReturnsNotFound(String operation) {
        authenticate(USER_ID, "USER");

        assertCode(ResponseCodeEnum.SUBSCRIPTION_NOT_FOUND, () -> close(operation));

        verify(subscriptionMapper, never()).update(isNull(), any());
    }

    @ParameterizedTest
    @CsvSource({"cancel, USER", "cancel, ADMIN", "refund, USER", "refund, ADMIN"})
    void anotherUserIncludingAdminCannotCancelOrRefundSubscription(String operation, String role) {
        authenticate(99L, role);
        when(subscriptionMapper.selectById(SUBSCRIPTION_ID))
                .thenReturn(subscription("ACTIVE", LocalDateTime.now().plusDays(1)));

        assertCode(ResponseCodeEnum.SUBSCRIPTION_NOT_FOUND, () -> close(operation));

        verify(subscriptionMapper, never()).update(isNull(), any());
    }

    @ParameterizedTest
    @CsvSource({"cancel, EXPIRED, 1", "cancel, CANCELED, 1", "cancel, REFUNDED, 1",
            "cancel, ACTIVE, -1", "cancel, ACTIVE,", "refund, EXPIRED, 1", "refund, CANCELED, 1",
            "refund, REFUNDED, 1", "refund, ACTIVE, -1", "refund, ACTIVE,"})
    void nonActiveExpiredOrUndatedSubscriptionsCannotBeClosed(String operation, String status, Integer days) {
        authenticate(USER_ID, "USER");
        LocalDateTime end = days == null ? null : LocalDateTime.now().plusDays(days);
        when(subscriptionMapper.selectById(SUBSCRIPTION_ID)).thenReturn(subscription(status, end));

        assertCode(ResponseCodeEnum.SUBSCRIPTION_NOT_ACTIVE, () -> close(operation));

        verify(subscriptionMapper, never()).update(isNull(), any());
    }

    private void close(String operation) {
        if ("cancel".equals(operation)) {
            assertTrue(service.cancel(SUBSCRIPTION_ID).isSuccess());
        } else if ("refund".equals(operation)) {
            assertTrue(service.refund(SUBSCRIPTION_ID).isSuccess());
        } else {
            throw new IllegalArgumentException(operation);
        }
    }

    private void assertCode(ResponseCodeEnum expected, Executable action) {
        assertEquals(expected.getErrorCode(), assertThrows(BizException.class, action).getErrorCode());
    }

    private void assertBetween(LocalDateTime actual, LocalDateTime before, LocalDateTime after) {
        assertNotNull(actual);
        assertFalse(actual.isBefore(before), "Timestamp precedes invocation");
        assertFalse(actual.isAfter(after), "Timestamp follows invocation");
    }

    private void authenticate(long id, String role) {
        LoginUser user = new LoginUser(id, "user-" + id, "unused", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private SubscriptionPlanDO plan(String status) {
        return SubscriptionPlanDO.builder().id(PLAN_ID).creatorId(CREATOR_ID).name("Monthly")
                .durationDays(30).price(new BigDecimal("29.90")).status(status).build();
    }

    private SubscriptionDO subscription(String status, LocalDateTime end) {
        return SubscriptionDO.builder().id(SUBSCRIPTION_ID).userId(USER_ID).creatorId(CREATOR_ID)
                .planId(PLAN_ID).startTime(LocalDateTime.now().minusDays(20)).endTime(end).status(status).build();
    }
}
