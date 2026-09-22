package com.contenthub.web.service.impl;

import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.service.ContentAccessService.AccessDecision;
import com.contenthub.web.service.SubscriptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentAccessServiceImplTest {
    @Mock SubscriptionService subscriptionService;
    @InjectMocks ContentAccessServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void freeContentAllowsAnonymousWithoutSubscriptionLookup() {
        AccessDecision decision = service.decide(content("FREE"), null);
        assertTrue(decision.full());
        assertNull(decision.lockReason());
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void subscriptionContentDeniesAnonymous() {
        AccessDecision decision = service.decide(content("SUBSCRIBED"), null);
        assertFalse(decision.full());
        assertNotNull(decision.lockReason());
        verifyNoInteractions(subscriptionService);
    }

    @ParameterizedTest
    @CsvSource({"10, CREATOR", "20, ADMIN"})
    void authorAndAdministratorDoNotNeedSubscription(long userId, String role) {
        AccessDecision decision = service.decide(content("SUBSCRIBED"), user(userId, role));
        assertTrue(decision.full());
        assertNull(decision.lockReason());
        verifyNoInteractions(subscriptionService);
    }

    @ParameterizedTest
    @CsvSource({"USER, true", "USER, false", "CREATOR, true", "CREATOR, false"})
    void otherReadersNeedActiveSubscriptionToThisCreator(String role, boolean active) {
        when(subscriptionService.hasActiveSubscription(20L, 10L)).thenReturn(active);
        AccessDecision decision = service.decide(content("SUBSCRIBED"), user(20L, role));
        assertEquals(active, decision.full());
        if (active) assertNull(decision.lockReason());
        else assertNotNull(decision.lockReason());
        verify(subscriptionService).hasActiveSubscription(20L, 10L);
        verifyNoMoreInteractions(subscriptionService);
    }

    private ContentDO content(String accessType) {
        return ContentDO.builder().id(1L).creatorId(10L).accessType(accessType).status("PUBLISHED").build();
    }

    private LoginUser user(long id, String role) {
        return new LoginUser(id, "user-" + id, "unused", role);
    }
}
