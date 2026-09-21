package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.contenthub.common.domain.dos.CreatorProfileDO;
import com.contenthub.common.domain.dos.SubscriptionDO;
import com.contenthub.common.domain.dos.SubscriptionPlanDO;
import com.contenthub.common.domain.mapper.CreatorProfileMapper;
import com.contenthub.common.domain.mapper.SubscriptionMapper;
import com.contenthub.common.domain.mapper.SubscriptionPlanMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.PageResponse;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.vo.SubscriptionVO;
import com.contenthub.web.service.SubscriptionService;
import com.contenthub.web.util.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionPlanMapper planMapper;
    private final CreatorProfileMapper creatorProfileMapper;

    public SubscriptionServiceImpl(SubscriptionMapper subscriptionMapper,
                                   SubscriptionPlanMapper planMapper,
                                   CreatorProfileMapper creatorProfileMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.planMapper = planMapper;
        this.creatorProfileMapper = creatorProfileMapper;
    }

    @Override
    @Transactional
    public Response<SubscriptionVO> payMock(Long planId) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        SubscriptionPlanDO plan = planMapper.selectById(planId);
        if (Objects.isNull(plan) || !"ACTIVE".equals(plan.getStatus())) {
            throw new BizException(ResponseCodeEnum.PLAN_NOT_FOUND);
        }
        if (Objects.equals(plan.getCreatorId(), loginUser.getUserId())) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), "不能订阅自己发布的套餐");
        }

        LocalDateTime now = LocalDateTime.now();

        // 同一创作者已有生效中的订阅 -> 续期而不是新建，避免「重复订阅」产生两条并行记录
        SubscriptionDO active = findActive(loginUser.getUserId(), plan.getCreatorId(), now);

        SubscriptionDO result;
        boolean renewed = false;
        if (active != null) {
            active.setPlanId(plan.getId());
            // 从原到期时间往后顺延，而不是从今天重算，否则提前续费会亏掉剩余天数
            active.setEndTime(active.getEndTime().plusDays(plan.getDurationDays()));
            subscriptionMapper.updateById(active);
            result = active;
            renewed = true;
        } else {
            result = SubscriptionDO.builder()
                    .userId(loginUser.getUserId())
                    .planId(plan.getId())
                    .creatorId(plan.getCreatorId())
                    .startTime(now)
                    .endTime(now.plusDays(plan.getDurationDays()))
                    .status("ACTIVE")
                    .build();
            subscriptionMapper.insert(result);
        }

        log.info("模拟支付成功：user={} plan={} 金额={} {}",
                loginUser.getUsername(), plan.getName(), plan.getPrice(), renewed ? "(续期)" : "(新订阅)");

        return Response.success(toVO(result, plan.getName(), creatorName(plan.getCreatorId())));
    }

    @Override
    public Response<PageResponse<SubscriptionVO>> mySubscriptions(long pageNum, long pageSize) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        Page<SubscriptionDO> page = subscriptionMapper.selectPage(
                Page.of(pageNum, pageSize),
                new LambdaQueryWrapper<SubscriptionDO>()
                        .eq(SubscriptionDO::getUserId, loginUser.getUserId())
                        .orderByDesc(SubscriptionDO::getCreateTime)
                        .orderByDesc(SubscriptionDO::getId));

        Map<Long, String> planNames = planNames(page.getRecords().stream()
                .map(SubscriptionDO::getPlanId).collect(Collectors.toSet()));
        Map<Long, String> creatorNames = creatorNames(page.getRecords().stream()
                .map(SubscriptionDO::getCreatorId).collect(Collectors.toSet()));

        return Response.success(PageResponse.of(page, sub -> toVO(
                sub, planNames.get(sub.getPlanId()), creatorNames.get(sub.getCreatorId()))));
    }

    @Override
    public boolean hasActiveSubscription(Long userId, Long creatorId) {
        if (userId == null || creatorId == null) {
            return false;
        }
        return findActive(userId, creatorId, LocalDateTime.now()) != null;
    }

    @Override
    public boolean hasAnyActiveSubscription(Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = subscriptionMapper.selectCount(new LambdaQueryWrapper<SubscriptionDO>()
                .eq(SubscriptionDO::getUserId, userId)
                .eq(SubscriptionDO::getStatus, "ACTIVE")
                .gt(SubscriptionDO::getEndTime, LocalDateTime.now()));
        return count != null && count > 0;
    }

    // ------------------------------------------------------------------ 内部方法

    private SubscriptionDO findActive(Long userId, Long creatorId, LocalDateTime now) {
        return subscriptionMapper.selectOne(new LambdaQueryWrapper<SubscriptionDO>()
                .eq(SubscriptionDO::getUserId, userId)
                .eq(SubscriptionDO::getCreatorId, creatorId)
                .eq(SubscriptionDO::getStatus, "ACTIVE")
                .gt(SubscriptionDO::getEndTime, now)
                .orderByDesc(SubscriptionDO::getEndTime)
                .last("LIMIT 1"));
    }

    private SubscriptionVO toVO(SubscriptionDO sub, String planName, String creatorName) {
        LocalDateTime now = LocalDateTime.now();
        boolean expiredByTime = sub.getEndTime() != null && !sub.getEndTime().isAfter(now);
        // 库里的 status 可能仍是 ACTIVE 但时间已过，展示与鉴权都按时间纠正
        String displayStatus = expiredByTime ? "EXPIRED" : sub.getStatus();
        boolean valid = "ACTIVE".equals(displayStatus);

        long remainingDays = 0;
        if (valid && sub.getEndTime() != null) {
            // 按「日期差」而不是「时长截断」算：刚买 7 天套餐时，
            // Duration.between(now, endTime).toDays() 因为毫秒差会返回 6，
            // 界面上就会显示成「剩余 6 天」，看起来像少了钱。
            remainingDays = Math.max(0,
                    ChronoUnit.DAYS.between(now.toLocalDate(), sub.getEndTime().toLocalDate()));
        }

        return SubscriptionVO.builder()
                .id(sub.getId())
                .planId(sub.getPlanId())
                .planName(planName)
                .creatorId(sub.getCreatorId())
                .creatorName(creatorName)
                .startTime(sub.getStartTime())
                .endTime(sub.getEndTime())
                .status(displayStatus)
                .remainingDays(remainingDays)
                .valid(valid)
                .createTime(sub.getCreateTime())
                .build();
    }

    private Map<Long, String> planNames(Set<Long> ids) {
        List<Long> list = ids.stream().filter(Objects::nonNull).toList();
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        return planMapper.selectBatchIds(list).stream()
                .collect(Collectors.toMap(SubscriptionPlanDO::getId, SubscriptionPlanDO::getName, (a, b) -> a));
    }

    private Map<Long, String> creatorNames(Set<Long> userIds) {
        List<Long> list = userIds.stream().filter(Objects::nonNull).toList();
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        return creatorProfileMapper.selectList(new LambdaQueryWrapper<CreatorProfileDO>()
                        .in(CreatorProfileDO::getUserId, list)).stream()
                .collect(Collectors.toMap(CreatorProfileDO::getUserId, CreatorProfileDO::getDisplayName, (a, b) -> a));
    }

    private String creatorName(Long creatorId) {
        CreatorProfileDO profile = creatorProfileMapper.selectOne(new LambdaQueryWrapper<CreatorProfileDO>()
                .eq(CreatorProfileDO::getUserId, creatorId));
        return profile == null ? null : profile.getDisplayName();
    }
}
