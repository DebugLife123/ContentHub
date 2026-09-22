import api from '../axios'
import type { ApiResponse, PageResult, Subscription } from './types'

/**
 * 模拟支付并创建/续期订阅（阶段 4 Day 34）。
 *
 * 后端没有接真实支付渠道，调用即视为支付成功。
 * 换成真实支付时，这里会变成「下单 -> 跳转支付 -> 回调后查询结果」。
 */
export function payMock(planId: number) {
  return api.post<ApiResponse<Subscription>>(`/subscriptions/${planId}/pay/mock`)
}

/** 我的订阅 */
export function mySubscriptions(pageNum = 1, pageSize = 10) {
  return api.get<ApiResponse<PageResult<Subscription>>>('/subscriptions/my', {
    params: { pageNum, pageSize },
  })
}

/**
 * 提前终止订阅。
 *
 * 终止后立即失去访问权限——后端鉴权条件是「status = ACTIVE 且 end_time 未过」，
 * 状态一变就不再有效，不需要等定时任务。
 */
export function cancelSubscription(id: number) {
  return api.post<ApiResponse<void>>(`/subscriptions/${id}/cancel`)
}

/** 模拟退款（置为 REFUNDED，同样立即失去权限） */
export function refundSubscription(id: number) {
  return api.post<ApiResponse<void>>(`/subscriptions/${id}/refund`)
}
