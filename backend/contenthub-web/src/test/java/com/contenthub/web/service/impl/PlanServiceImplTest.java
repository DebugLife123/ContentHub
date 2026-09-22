package com.contenthub.web.service.impl;

import com.contenthub.common.domain.dos.SubscriptionPlanDO;
import com.contenthub.common.domain.mapper.CreatorProfileMapper;
import com.contenthub.common.domain.mapper.SubscriptionMapper;
import com.contenthub.common.domain.mapper.SubscriptionPlanMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.req.PlanReqVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceImplTest {
    @Mock SubscriptionPlanMapper planMapper;
    @Mock SubscriptionMapper subscriptionMapper;
    @Mock CreatorProfileMapper creatorProfileMapper;
    @InjectMocks PlanServiceImpl service;

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test
    void anonymousCannotListPrivatePlans() {
        assertCode(ResponseCodeEnum.UNAUTHORIZED, () -> service.listMine());
        verifyNoInteractions(planMapper, subscriptionMapper, creatorProfileMapper);
    }

    @Test
    void otherCreatorCannotUpdateOrDeletePlan() {
        login(20L, "CREATOR");
        when(planMapper.selectById(1L)).thenReturn(plan());
        assertCode(ResponseCodeEnum.FORBIDDEN, () -> service.update(1L, request()));
        assertCode(ResponseCodeEnum.FORBIDDEN, () -> service.delete(1L));
        verify(planMapper, never()).updateById(any(SubscriptionPlanDO.class));
        verify(planMapper, never()).deleteById(anyLong());
        verifyNoInteractions(subscriptionMapper, creatorProfileMapper);
    }

    @ParameterizedTest
    @CsvSource({"10, CREATOR", "20, ADMIN"})
    void ownerAndAdministratorCanDeleteUnusedPlan(long userId, String role) {
        login(userId, role);
        when(planMapper.selectById(1L)).thenReturn(plan());
        when(subscriptionMapper.selectCount(any())).thenReturn(0L);
        assertDoesNotThrow(() -> service.delete(1L));
        verify(planMapper).deleteById(1L);
    }

    @ParameterizedTest
    @CsvSource({"10, CREATOR", "20, ADMIN"})
    void existingSubscriptionPreventsDeletionEvenForAdministrator(long userId, String role) {
        login(userId, role);
        when(planMapper.selectById(1L)).thenReturn(plan());
        when(subscriptionMapper.selectCount(any())).thenReturn(1L);
        assertCode(ResponseCodeEnum.PLAN_IN_USE, () -> service.delete(1L));
        verify(planMapper, never()).deleteById(anyLong());
    }

    @Test
    void missingPlanDoesNotWriteAnything() {
        assertCode(ResponseCodeEnum.PLAN_NOT_FOUND, () -> service.delete(404L));
        assertCode(ResponseCodeEnum.PLAN_NOT_FOUND, () -> service.update(404L, request()));
        verify(planMapper, never()).deleteById(anyLong());
        verify(planMapper, never()).updateById(any(SubscriptionPlanDO.class));
        verifyNoInteractions(subscriptionMapper);
    }

    private SubscriptionPlanDO plan() {
        return SubscriptionPlanDO.builder().id(1L).creatorId(10L).name("Plan")
                .status("ACTIVE").price(BigDecimal.TEN).durationDays(30).build();
    }

    private PlanReqVO request() {
        PlanReqVO req = new PlanReqVO();
        req.setName(" Revised plan ");
        req.setPrice(BigDecimal.TEN);
        req.setDurationDays(30);
        return req;
    }

    private void login(long userId, String role) {
        LoginUser user = new LoginUser(userId, "user-" + userId, "unused", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private void assertCode(ResponseCodeEnum expected, org.junit.jupiter.api.function.Executable action) {
        assertEquals(expected.getErrorCode(), assertThrows(BizException.class, action).getErrorCode());
    }
}
