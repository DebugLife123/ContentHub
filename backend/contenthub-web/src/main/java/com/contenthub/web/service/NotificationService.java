package com.contenthub.web.service;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.NotificationVO;

/**
 * 站内通知。
 *
 * <p>只做「写一条 + 读自己的」，没有推送通道——它的价值在于把审核结果
 * 主动告诉当事人，而不是让创作者自己去工作台翻 {@code reject_reason}。</p>
 */
public interface NotificationService {

    /**
     * 写一条通知。
     *
     * <p>刻意不抛异常：通知只是副作用，借这个由头发通知失败不应该把
     * 「审核通过」这个主流程一起回滚掉。</p>
     */
    void push(Long userId, String type, String title, String body, String bizType, Long bizId);

    /** 我的通知（可按已读状态筛选） */
    Response<PageResponse<NotificationVO>> myNotifications(long pageNum, long pageSize, Boolean unreadOnly);

    /** 未读条数，给前端角标用 */
    Response<Long> unreadCount();

    /** 标记单条已读 */
    Response<Void> markRead(Long id);

    /** 全部标记已读 */
    Response<Void> markAllRead();
}
