import api from '../axios'
import type { ApiResponse, PageResult, Subscription } from './types'

/**
 * 生成幂等键。
 *
 * 刻意不直接用 `crypto.randomUUID()`：它只在**安全上下文**（HTTPS / localhost）可用，
 * 而本项目线上是 `http://<ip>:<port>`，浏览器里 `crypto.randomUUID` 会是 undefined，
 * 本地开发却一切正常——典型的「本地能跑、线上炸」。
 */
export function newIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `k-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

/**
 * 模拟支付并创建/续期订阅（阶段 4 Day 34）。
 *
 * 后端没有接真实支付渠道，调用即视为支付成功。
 * 换成真实支付时，这里会变成「下单 -> 跳转支付 -> 回调后查询结果」。
 *
 * `Idempotency-Key` 是后端必填头：同一个 key 重复提交只会生效一次，
 * 避免「请求超时但服务端已提交，用户再点一次」被当成两次购买而重复续期。
 * 重试场景要复用同一个 key，所以由调用方传入。
 */
export function payMock(planId: number, idempotencyKey: string) {
  return api.post<ApiResponse<Subscription>>(`/subscriptions/${planId}/pay/mock`, null, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
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
