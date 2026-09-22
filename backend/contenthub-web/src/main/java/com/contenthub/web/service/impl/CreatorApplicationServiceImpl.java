package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.contenthub.common.domain.dos.CreatorApplicationDO;
import com.contenthub.common.domain.dos.CreatorProfileDO;
import com.contenthub.common.domain.dos.UserDO;
import com.contenthub.common.domain.mapper.CreatorApplicationMapper;
import com.contenthub.common.domain.mapper.CreatorProfileMapper;
import com.contenthub.common.domain.mapper.UserMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.req.CreatorApplyReqVO;
import com.contenthub.web.model.vo.CreatorApplicationVO;
import com.contenthub.web.service.CreatorApplicationService;
import com.contenthub.web.service.NotificationService;
import com.contenthub.web.util.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CreatorApplicationServiceImpl implements CreatorApplicationService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final CreatorApplicationMapper applicationMapper;
    private final UserMapper userMapper;
    private final CreatorProfileMapper creatorProfileMapper;
    private final NotificationService notificationService;

    public CreatorApplicationServiceImpl(CreatorApplicationMapper applicationMapper,
                                         UserMapper userMapper,
                                         CreatorProfileMapper creatorProfileMapper,
                                         NotificationService notificationService) {
        this.applicationMapper = applicationMapper;
        this.userMapper = userMapper;
        this.creatorProfileMapper = creatorProfileMapper;
        this.notificationService = notificationService;
    }

    // ------------------------------------------------------------------ 用户侧

    @Override
    @Transactional
    public Response<CreatorApplicationVO> apply(CreatorApplyReqVO req) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        if ("CREATOR".equals(loginUser.getRole()) || "ADMIN".equals(loginUser.getRole())) {
            throw new BizException(ResponseCodeEnum.ALREADY_CREATOR);
        }

        // 已经有一条待审核的就不要再堆了，否则管理员会看到同一个人的多条申请
        CreatorApplicationDO pending = applicationMapper.selectOne(
                new LambdaQueryWrapper<CreatorApplicationDO>()
                        .eq(CreatorApplicationDO::getUserId, loginUser.getUserId())
                        .eq(CreatorApplicationDO::getStatus, PENDING)
                        .orderByDesc(CreatorApplicationDO::getId)
                        .last("LIMIT 1"));
        if (pending != null) {
            throw new BizException(ResponseCodeEnum.CREATOR_APPLICATION_EXISTS);
        }

        CreatorApplicationDO application = CreatorApplicationDO.builder()
                .userId(loginUser.getUserId())
                .intro(req == null ? null : req.getIntro())
                .status(PENDING)
                .build();
        applicationMapper.insert(application);

        log.info("用户 {} 提交创作者申请 id={}", loginUser.getUsername(), application.getId());
        return Response.success(toVO(application, usernameOf(application.getUserId())));
    }

    @Override
    public Response<CreatorApplicationVO> myApplication() {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        CreatorApplicationDO latest = applicationMapper.selectOne(
                new LambdaQueryWrapper<CreatorApplicationDO>()
                        .eq(CreatorApplicationDO::getUserId, loginUser.getUserId())
                        .orderByDesc(CreatorApplicationDO::getId)
                        .last("LIMIT 1"));
        // 没申请过不是错误，返回 null 让前端显示「可以申请」
        return Response.success(latest == null ? null : toVO(latest, loginUser.getUsername()));
    }

    // ------------------------------------------------------------------ 管理端

    @Override
    public Response<PageResponse<CreatorApplicationVO>> pageForReview(long pageNum, long pageSize, String status) {
        // 默认只看待审核队列，和内容审核保持一致
        String effective = StringUtils.isBlank(status) ? PENDING : status;

        LambdaQueryWrapper<CreatorApplicationDO> query = new LambdaQueryWrapper<CreatorApplicationDO>()
                .eq(CreatorApplicationDO::getStatus, effective)
                .orderByAsc(CreatorApplicationDO::getId);

        Page<CreatorApplicationDO> page = applicationMapper.selectPage(new Page<>(pageNum, pageSize), query);
        Map<Long, String> usernames = usernames(page.getRecords());
        return Response.success(PageResponse.of(page, a -> toVO(a, usernames.get(a.getUserId()))));
    }

    @Override
    @Transactional
    public Response<Void> approve(Long id) {
        CreatorApplicationDO application = requirePending(id);
        LoginUser reviewer = CurrentUserUtil.requireLoginUser();

        applicationMapper.updateById(CreatorApplicationDO.builder()
                .id(id)
                .status(APPROVED)
                .reviewerId(reviewer.getUserId())
                .reviewTime(LocalDateTime.now())
                .build());

        UserDO user = userMapper.selectById(application.getUserId());
        if (user == null) {
            throw new BizException(ResponseCodeEnum.UNAUTHORIZED);
        }

        userMapper.updateById(UserDO.builder().id(user.getId()).role("CREATOR").build());

        // 有角色没资料会出现空状态，这里顺手补一条（与 CreatorServiceImpl.loadOrCreate 同一套约定）
        CreatorProfileDO profile = creatorProfileMapper.selectOne(new LambdaQueryWrapper<CreatorProfileDO>()
                .eq(CreatorProfileDO::getUserId, user.getId()));
        if (profile == null) {
            creatorProfileMapper.insert(CreatorProfileDO.builder()
                    .userId(user.getId())
                    .displayName(user.getNickname() != null ? user.getNickname() : user.getUsername())
                    .verified(false)
                    .subscriberCount(0)
                    .contentCount(0)
                    .build());
        }

        notificationService.push(user.getId(), "CREATOR_APPROVED", "创作者申请已通过",
                "你现在可以发布内容、创建订阅套餐了。", "CREATOR_APPLICATION", id);

        log.info("管理员 {} 通过了创作者申请 {}（用户 {}）", reviewer.getUsername(), id, user.getUsername());
        return Response.success();
    }

    @Override
    @Transactional
    public Response<Void> reject(Long id, String reason) {
        CreatorApplicationDO application = requirePending(id);
        LoginUser reviewer = CurrentUserUtil.requireLoginUser();

        applicationMapper.updateById(CreatorApplicationDO.builder()
                .id(id)
                .status(REJECTED)
                .rejectReason(reason)
                .reviewerId(reviewer.getUserId())
                .reviewTime(LocalDateTime.now())
                .build());

        notificationService.push(application.getUserId(), "CREATOR_REJECTED", "创作者申请未通过",
                StringUtils.isBlank(reason) ? "管理员未填写原因，可修改资料后重新申请。" : reason,
                "CREATOR_APPLICATION", id);

        log.info("管理员 {} 驳回了创作者申请 {}，原因：{}", reviewer.getUsername(), id, reason);
        return Response.success();
    }

    @Override
    public Response<Long> pendingCount() {
        Long count = applicationMapper.selectCount(new LambdaQueryWrapper<CreatorApplicationDO>()
                .eq(CreatorApplicationDO::getStatus, PENDING));
        return Response.success(count == null ? 0L : count);
    }

    // ------------------------------------------------------------------ 内部

    private CreatorApplicationDO requirePending(Long id) {
        CreatorApplicationDO application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BizException(ResponseCodeEnum.CREATOR_APPLICATION_NOT_FOUND);
        }
        if (!PENDING.equals(application.getStatus())) {
            throw new BizException(ResponseCodeEnum.APPLICATION_STATUS_ILLEGAL);
        }
        return application;
    }

    private String usernameOf(Long userId) {
        UserDO user = userMapper.selectById(userId);
        return user == null ? null : user.getUsername();
    }

    /** 一次查出本页涉及的用户，避免逐行查库 */
    private Map<Long, String> usernames(List<CreatorApplicationDO> applications) {
        Set<Long> ids = applications.stream()
                .map(CreatorApplicationDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = new HashMap<>();
        for (UserDO user : userMapper.selectBatchIds(ids)) {
            names.put(user.getId(), user.getUsername());
        }
        return names;
    }

    private CreatorApplicationVO toVO(CreatorApplicationDO a, String username) {
        UserDO user = username == null ? userMapper.selectById(a.getUserId()) : null;
        return CreatorApplicationVO.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .username(username != null ? username : (user == null ? null : user.getUsername()))
                .nickname(user == null ? null : user.getNickname())
                .intro(a.getIntro())
                .status(a.getStatus())
                .rejectReason(a.getRejectReason())
                .reviewerId(a.getReviewerId())
                .reviewTime(a.getReviewTime())
                .createTime(a.getCreateTime())
                .build();
    }
}
