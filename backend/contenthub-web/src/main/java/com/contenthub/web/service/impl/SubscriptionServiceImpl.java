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
import com.contenthub.web.model.vo.CreatorRevenueVO;
import com.contenthub.web.model.vo.SubscriptionVO;
import com.contenthub.web.service.SubscriptionService;
import com.contenthub.web.util.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
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

    // ------------------------------------------------------------------ 终止 / 退款

    @Override
    @Transactional
    public Response<Void> cancel(Long subscriptionId) {
        return close(subscriptionId, "CANCELED");
    }

    @Override
    @Transactional
    public Response<Void> refund(Long subscriptionId) {
        return close(subscriptionId, "REFUNDED");
    }

    /**
     * 终止与退款只差一个目标状态，走同一个入口。
     *
     * <p>两个状态都会让 {@code hasActiveSubscription} 立刻返回 false
     * （它要求 status = ACTIVE），所以不需要额外的失效逻辑。</p>
     */
    private Response<Void> close(Long subscriptionId, String target) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        SubscriptionDO sub = subscriptionMapper.selectById(subscriptionId);
        if (sub == null) {
            throw new BizException(ResponseCodeEnum.SUBSCRIPTION_NOT_FOUND);
        }
        // 只能操作自己的订阅。这里对管理员也一视同仁：
        // 退款是资金动作，不适合由管理员替用户点。
        if (!Objects.equals(sub.getUserId(), loginUser.getUserId())) {
            throw new BizException(ResponseCodeEnum.SUBSCRIPTION_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean active = "ACTIVE".equals(sub.getStatus())
                && sub.getEndTime() != null && sub.getEndTime().isAfter(now);
        if (!active) {
            throw new BizException(ResponseCodeEnum.SUBSCRIPTION_NOT_ACTIVE);
        }

        subscriptionMapper.updateById(SubscriptionDO.builder()
                .id(subscriptionId)
                .status(target)
                .closedTime(now)
                .build());

        log.info("订阅 {} 用户 {} {}（原到期时间 {}）", subscriptionId, loginUser.getUsername(),
                "REFUNDED".equals(target) ? "申请退款" : "提前终止", sub.getEndTime());
        return Response.success();
    }

    // ------------------------------------------------------------------ 创作者收益

    @Override
    public Response<CreatorRevenueVO> revenue() {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        List<SubscriptionDO> subs = subscriptionMapper.selectList(
                new LambdaQueryWrapper<SubscriptionDO>()
                        .eq(SubscriptionDO::getCreatorId, loginUser.getUserId())
                        .orderByDesc(SubscriptionDO::getCreateTime)
                        .orderByDesc(SubscriptionDO::getId));

        Map<Long, SubscriptionPlanDO> plans = plansOf(subs);
        LocalDateTime now = LocalDateTime.now();

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        long activeCount = 0;

        Map<Long, Long> countByPlan = new LinkedHashMap<>();
        Map<Long, BigDecimal> amountByPlan = new LinkedHashMap<>();
        Map<String, Long> countByMonth = new TreeMap<>(Comparator.reverseOrder());
        Map<String, BigDecimal> amountByMonth = new TreeMap<>(Comparator.reverseOrder());

        for (SubscriptionDO sub : subs) {
            SubscriptionPlanDO plan = plans.get(sub.getPlanId());
            BigDecimal price = plan == null || plan.getPrice() == null ? BigDecimal.ZERO : plan.getPrice();

            if ("ACTIVE".equals(sub.getStatus())
                    && sub.getEndTime() != null && sub.getEndTime().isAfter(now)) {
                activeCount++;
            }

            // 退款单不算收益，也不进套餐/月度拆分，只单独累计退款金额
            if ("REFUNDED".equals(sub.getStatus())) {
                refunded = refunded.add(price);
                continue;
            }

            total = total.add(price);
            countByPlan.merge(sub.getPlanId(), 1L, Long::sum);
            amountByPlan.merge(sub.getPlanId(), price, BigDecimal::add);

            String month = sub.getCreateTime() == null
                    ? "未知"
                    : sub.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            countByMonth.merge(month, 1L, Long::sum);
            amountByMonth.merge(month, price, BigDecimal::add);
        }

        List<CreatorRevenueVO.PlanRevenue> byPlan = countByPlan.entrySet().stream()
                .map(e -> {
                    SubscriptionPlanDO plan = plans.get(e.getKey());
                    return CreatorRevenueVO.PlanRevenue.builder()
                            .planId(e.getKey())
                            .planName(plan == null ? "已删除的套餐" : plan.getName())
                            .price(plan == null ? BigDecimal.ZERO : plan.getPrice())
                            .count(e.getValue())
                            .amount(amountByPlan.getOrDefault(e.getKey(), BigDecimal.ZERO))
                            .build();
                })
                .toList();

        List<CreatorRevenueVO.MonthlyRevenue> monthly = countByMonth.entrySet().stream()
                .map(e -> CreatorRevenueVO.MonthlyRevenue.builder()
                        .month(e.getKey())
                        .count(e.getValue())
                        .amount(amountByMonth.getOrDefault(e.getKey(), BigDecimal.ZERO))
                        .build())
                .toList();

        String creatorName = creatorName(loginUser.getUserId());
        List<SubscriptionVO> recent = subs.stream()
                .limit(10)
                .map(s -> {
                    SubscriptionPlanDO plan = plans.get(s.getPlanId());
                    return toVO(s, plan == null ? null : plan.getName(), creatorName);
                })
                .toList();

        return Response.success(CreatorRevenueVO.builder()
                .totalRevenue(total)
                .refundedAmount(refunded)
                .subscriptionCount(subs.size())
                .activeCount(activeCount)
                .byPlan(byPlan)
                .monthly(monthly)
                .recent(recent)
                .build());
    }

    private Map<Long, SubscriptionPlanDO> plansOf(List<SubscriptionDO> subs) {
        Set<Long> ids = subs.stream().map(SubscriptionDO::getPlanId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return planMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SubscriptionPlanDO::getId, p -> p, (a, b) -> a));
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
