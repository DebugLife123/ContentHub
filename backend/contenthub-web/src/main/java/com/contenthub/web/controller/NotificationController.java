package com.contenthub.web.controller;

import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.web.model.vo.NotificationVO;
import com.contenthub.web.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 站内通知（只涉及当前登录用户自己的） */
@RestController
@RequestMapping("/notifications")
@Tag(name = "站内通知")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "我的通知（unreadOnly=true 只看未读）")
    public Response<PageResponse<NotificationVO>> myNotifications(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Boolean unreadOnly) {
        return notificationService.myNotifications(pageNum, pageSize, unreadOnly);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读条数（顶部角标用）")
    public Response<Long> unreadCount() {
        return notificationService.unreadCount();
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记单条已读")
    public Response<Void> markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public Response<Void> markAllRead() {
        return notificationService.markAllRead();
    }
}
