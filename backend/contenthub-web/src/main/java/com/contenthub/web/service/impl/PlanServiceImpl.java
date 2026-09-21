package com.contenthub.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.contenthub.common.domain.dos.CreatorProfileDO;
import com.contenthub.common.domain.dos.SubscriptionDO;
import com.contenthub.common.domain.dos.SubscriptionPlanDO;
import com.contenthub.common.domain.mapper.CreatorProfileMapper;
import com.contenthub.common.domain.mapper.SubscriptionMapper;
import com.contenthub.common.domain.mapper.SubscriptionPlanMapper;
import com.contenthub.common.enums.ResponseCodeEnum;
import com.contenthub.common.exception.BizException;
import com.contenthub.common.utils.Response;
import com.contenthub.jwt.model.LoginUser;
import com.contenthub.web.model.req.PlanReqVO;
import com.contenthub.web.model.vo.SubscriptionPlanVO;
import com.contenthub.web.service.PlanService;
import com.contenthub.web.util.CurrentUserUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlanServiceImpl implements PlanService {

    private final SubscriptionPlanMapper planMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final CreatorProfileMapper creatorProfileMapper;

    public PlanServiceImpl(SubscriptionPlanMapper planMapper,
                           SubscriptionMapper subscriptionMapper,
                           CreatorProfileMapper creatorProfileMapper) {
        this.planMapper = planMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.creatorProfileMapper = creatorProfileMapper;
    }

    @Override
    public Response<List<SubscriptionPlanVO>> listActive() {
        List<SubscriptionPlanDO> plans = planMapper.selectList(new LambdaQueryWrapper<SubscriptionPlanDO>()
                .eq(SubscriptionPlanDO::getStatus, "ACTIVE")
                .orderByAsc(SubscriptionPlanDO::getPrice)
                .orderByAsc(SubscriptionPlanDO::getId));
        return Response.success(toVOList(plans));
    }

    @Override
    public Response<List<SubscriptionPlanVO>> listMine() {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        List<SubscriptionPlanDO> plans = planMapper.selectList(new LambdaQueryWrapper<SubscriptionPlanDO>()
                .eq(SubscriptionPlanDO::getCreatorId, loginUser.getUserId())
                .orderByDesc(SubscriptionPlanDO::getId));
        return Response.success(toVOList(plans));
    }

    @Override
    @Transactional
    public Response<SubscriptionPlanVO> create(PlanReqVO req) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();

        SubscriptionPlanDO plan = SubscriptionPlanDO.builder()
                .creatorId(loginUser.getUserId())
                .name(req.getName().trim())
                .description(req.getDescription())
                .price(req.getPrice())
                .durationDays(req.getDurationDays())
                .status(StringUtils.defaultIfBlank(req.getStatus(), "ACTIVE").toUpperCase())
                .build();

        planMapper.insert(plan);
        return Response.success(toVOList(List.of(plan)).get(0));
    }

    @Override
    @Transactional
    public Response<SubscriptionPlanVO> update(Long id, PlanReqVO req) {
        SubscriptionPlanDO existing = requirePlan(id);
        requireOwnership(existing);

        SubscriptionPlanDO update = SubscriptionPlanDO.builder()
                .id(id)
                .name(req.getName().trim())
                .description(req.getDescription())
                .price(req.getPrice())
                .durationDays(req.getDurationDays())
                .status(StringUtils.defaultIfBlank(req.getStatus(), existing.getStatus()).toUpperCase())
                .build();

        planMapper.updateById(update);
        return Response.success(toVOList(List.of(update)).get(0));
    }

    @Override
    @Transactional
    public Response<Void> delete(Long id) {
        SubscriptionPlanDO existing = requirePlan(id);
        requireOwnership(existing);

        // 已有订阅记录的套餐不能删，否则「我的订阅」里会出现查不到套餐名的记录
        Long used = subscriptionMapper.selectCount(new LambdaQueryWrapper<SubscriptionDO>()
                .eq(SubscriptionDO::getPlanId, id));
        if (used != null && used > 0) {
            throw new BizException(ResponseCodeEnum.PLAN_IN_USE);
        }

        planMapper.deleteById(id);
        return Response.success();
    }

    @Override
    public Response<List<SubscriptionPlanVO>> listAll() {
        List<SubscriptionPlanDO> plans = planMapper.selectList(new LambdaQueryWrapper<SubscriptionPlanDO>()
                .orderByDesc(SubscriptionPlanDO::getId));
        return Response.success(toVOList(plans));
    }

    // ------------------------------------------------------------------ 内部方法

    private SubscriptionPlanDO requirePlan(Long id) {
        SubscriptionPlanDO plan = planMapper.selectById(id);
        if (Objects.isNull(plan)) {
            throw new BizException(ResponseCodeEnum.PLAN_NOT_FOUND);
        }
        return plan;
    }

    /** 套餐只能由所属创作者或管理员维护 */
    private void requireOwnership(SubscriptionPlanDO plan) {
        LoginUser loginUser = CurrentUserUtil.requireLoginUser();
        if ("ADMIN".equals(loginUser.getRole())) {
            return;
        }
        if (!Objects.equals(plan.getCreatorId(), loginUser.getUserId())) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN);
        }
    }

    private List<SubscriptionPlanVO> toVOList(List<SubscriptionPlanDO> plans) {
        if (plans.isEmpty()) {
            return List.of();
        }

        Set<Long> creatorIds = plans.stream().map(SubscriptionPlanDO::getCreatorId).collect(Collectors.toSet());
        Map<Long, String> names = creatorNames(creatorIds);

        Set<Long> planIds = plans.stream().map(SubscriptionPlanDO::getId).collect(Collectors.toSet());
        Map<Long, Long> counts = subscriberCounts(planIds);

        return plans.stream()
                .map(p -> SubscriptionPlanVO.builder()
                        .id(p.getId())
                        .creatorId(p.getCreatorId())
                        .creatorName(names.get(p.getCreatorId()))
                        .name(p.getName())
                        .description(p.getDescription())
                        .price(p.getPrice())
                        .durationDays(p.getDurationDays())
                        .status(p.getStatus())
                        .subscriberCount(counts.getOrDefault(p.getId(), 0L))
                        .createTime(p.getCreateTime())
                        .updateTime(p.getUpdateTime())
                        .build())
                .toList();
    }

    private Map<Long, String> creatorNames(Set<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return creatorProfileMapper.selectList(new LambdaQueryWrapper<CreatorProfileDO>()
                        .in(CreatorProfileDO::getUserId, ids)).stream()
                .collect(Collectors.toMap(CreatorProfileDO::getUserId, CreatorProfileDO::getDisplayName, (a, b) -> a));
    }

    private Map<Long, Long> subscriberCounts(Set<Long> planIds) {
        if (planIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SubscriptionDO> subs = subscriptionMapper.selectList(new LambdaQueryWrapper<SubscriptionDO>()
                .select(SubscriptionDO::getPlanId)
                .in(SubscriptionDO::getPlanId, planIds));
        return subs.stream().collect(Collectors.groupingBy(SubscriptionDO::getPlanId, Collectors.counting()));
    }
}
