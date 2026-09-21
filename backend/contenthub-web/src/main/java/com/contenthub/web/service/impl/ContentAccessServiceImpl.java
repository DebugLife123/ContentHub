package com.contenthub.web.service.impl;

import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.service.ContentAccessService;
import com.contenthub.web.service.SubscriptionService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ContentAccessServiceImpl implements ContentAccessService {

    private final SubscriptionService subscriptionService;

    public ContentAccessServiceImpl(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    public AccessDecision decide(ContentDO content, LoginUser loginUser) {
        // 1. 免费内容对所有人开放（计划 Day 25）
        if ("FREE".equals(content.getAccessType())) {
            return AccessDecision.allow();
        }

        // 2. 未登录用户只能看试读
        if (loginUser == null) {
            return AccessDecision.deny("该内容为订阅专属，登录并订阅后可阅读全文");
        }

        // 3. 作者本人与管理员始终可看（管理员要能审内容）
        if ("ADMIN".equals(loginUser.getRole())
                || Objects.equals(content.getCreatorId(), loginUser.getUserId())) {
            return AccessDecision.allow();
        }

        // 4. 对该作者持有有效订阅（计划 Day 36：订阅过期立即失去权限）
        if (subscriptionService.hasActiveSubscription(loginUser.getUserId(), content.getCreatorId())) {
            return AccessDecision.allow();
        }

        return AccessDecision.deny("该内容需要订阅创作者后阅读，订阅到期后会自动重新锁定");
    }
}
