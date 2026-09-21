package com.contenthub.web.service;

import com.contenthub.common.domain.dos.ContentDO;
import com.contenthub.jwt.model.LoginUser;

/**
 * 内容访问权限判断（计划 Day 25-27 与 Day 36）。
 *
 * <p>把「能不能看正文」这件事收敛到一个地方，避免在各个 Controller 里散落 if。
 * 判定顺序：免费 → 作者/管理员 → 有效订阅 → 否则只给试读。</p>
 */
public interface ContentAccessService {

    /**
     * @param content 已确认存在的内容
     * @return 访问决策
     */
    AccessDecision decide(ContentDO content, LoginUser loginUser);

    /** 试读片段长度（字符） */
    int PREVIEW_LENGTH = 120;

    /**
     * @param full       是否可以看到完整正文与附件
     * @param lockReason 不可见时的原因，用于前端展示订阅引导
     */
    record AccessDecision(boolean full, String lockReason) {
        public static AccessDecision allow() {
            return new AccessDecision(true, null);
        }

        public static AccessDecision deny(String reason) {
            return new AccessDecision(false, reason);
        }
    }
}
