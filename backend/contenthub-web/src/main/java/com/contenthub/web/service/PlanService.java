package com.contenthub.web.service;

import com.contenthub.common.utils.Response;
import com.contenthub.web.model.req.PlanReqVO;
import com.contenthub.web.model.vo.SubscriptionPlanVO;

import java.util.List;

public interface PlanService {

    /** 公开：已上架的套餐（计划表 19：GET /api/plans） */
    Response<List<SubscriptionPlanVO>> listActive();

    /** 创作者查看自己的套餐（含已下架） */
    Response<List<SubscriptionPlanVO>> listMine();

    Response<SubscriptionPlanVO> create(PlanReqVO req);

    Response<SubscriptionPlanVO> update(Long id, PlanReqVO req);

    Response<Void> delete(Long id);
}
