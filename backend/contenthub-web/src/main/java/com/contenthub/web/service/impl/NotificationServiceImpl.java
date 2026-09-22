package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.contenthub.common.domain.dos.NotificationDO;
import com.contenthub.common.domain.mapper.NotificationMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.vo.NotificationVO;
import com.contenthub.web.service.NotificationService;
import com.contenthub.web.util.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final int UNREAD = 0;
    private static final int READ = 1;

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public void push(Long userId, String type, String title, String body, String bizType, Long bizId) {
        if (userId == null) {
            return;
        }
        try {
            notificationMapper.insert(NotificationDO.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .body(body)
                    .bizType(bizType)
                    .bizId(bizId)
                    .readFlag(UNREAD)
                    .build());
        } catch (Exception e) {
            // 通知是主流程的副作用，写失败只记日志，不能让「审核通过」跟着失败
            log.warn("写通知失败 userId={} type={}", userId, type, e);
        }
    }

    @Override
    public Response<PageResponse<NotificationVO>> myNotifications(long pageNum, long pageSize, Boolean unreadOnly) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        LambdaQueryWrapper<NotificationDO> query = new LambdaQueryWrapper<NotificationDO>()
                .eq(NotificationDO::getUserId, loginUser.getUserId())
                .orderByDesc(NotificationDO::getCreateTime)
                .orderByDesc(NotificationDO::getId);
        if (Boolean.TRUE.equals(unreadOnly)) {
            query.eq(NotificationDO::getReadFlag, UNREAD);
        }

        Page<NotificationDO> page = notificationMapper.selectPage(new Page<>(pageNum, pageSize), query);
        return Response.success(PageResponse.of(page, this::toVO));
    }

    @Override
    public Response<Long> unreadCount() {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<NotificationDO>()
                .eq(NotificationDO::getUserId, loginUser.getUserId())
                .eq(NotificationDO::getReadFlag, UNREAD));
        return Response.success(count == null ? 0L : count);
    }

    @Override
    @Transactional
    public Response<Void> markRead(Long id) {
        NotificationDO notification = requireMine(id);
        if (Objects.equals(notification.getReadFlag(), READ)) {
            return Response.success();
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<NotificationDO>()
                .eq(NotificationDO::getId, id)
                .set(NotificationDO::getReadFlag, READ));
        return Response.success();
    }

    @Override
    @Transactional
    public Response<Void> markAllRead() {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        notificationMapper.update(null, new LambdaUpdateWrapper<NotificationDO>()
                .eq(NotificationDO::getUserId, loginUser.getUserId())
                .eq(NotificationDO::getReadFlag, UNREAD)
                .set(NotificationDO::getReadFlag, READ));
        return Response.success();
    }

    /** 只能读自己的通知——否则改个 id 就能看别人的审核结果 */
    private NotificationDO requireMine(Long id) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        NotificationDO notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BizException(ResponseCodeEnum.NOTIFICATION_NOT_FOUND);
        }
        if ("ADMIN".equals(loginUser.getRole())) {
            return notification;
        }
        if (!Objects.equals(notification.getUserId(), loginUser.getUserId())) {
            throw new BizException(ResponseCodeEnum.NOTIFICATION_NOT_FOUND);
        }
        return notification;
    }

    private NotificationVO toVO(NotificationDO n) {
        return NotificationVO.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .bizType(n.getBizType())
                .bizId(n.getBizId())
                .read(Objects.equals(n.getReadFlag(), READ))
                .createTime(n.getCreateTime())
                .build();
    }
}
