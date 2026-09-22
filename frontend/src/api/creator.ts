import api from '../axios'
import type { ApiResponse, CreatorApplication, CreatorDashboard, CreatorProfile, CreatorRevenue } from './types'

/**
 * 提交创作者申请。
 *
 * 注意语义变了：以前调用即获得 CREATOR 角色，现在只是提交一条待审核记录，
 * 管理员通过之后 `users.role` 才会变成 CREATOR。所以前端必须按
 * 「未申请 / 待审核 / 已驳回 / 已通过」四种状态分别渲染。
 */
export function applyCreator(intro?: string) {
  return api.post<ApiResponse<CreatorApplication>>('/creator/apply', { intro })
}

/** 我的最近一条申请；从没申请过时 data 为 null */
export function getMyCreatorApplication() {
  return api.get<ApiResponse<CreatorApplication | null>>('/creator/application')
}

/** 我的创作者资料 */
export function getMyCreatorProfile() {
  return api.get<ApiResponse<CreatorProfile>>('/creator/profile')
}

/** 修改创作者资料 */
export function updateCreatorProfile(payload: { displayName: string; intro?: string | null }) {
  return api.put<ApiResponse<CreatorProfile>>('/creator/profile', payload)
}

/** 创作者公开资料 */
export function getCreatorProfile(userId: number) {
  return api.get<ApiResponse<CreatorProfile>>(`/creators/${userId}`)
}

/** 创作者仪表盘统计（阶段 6 Day 48） */
export function getCreatorDashboard() {
  return api.get<ApiResponse<CreatorDashboard>>('/creator/dashboard')
}

/**
 * 创作者收益。
 *
 * 口径：已支付且未退款的订阅金额。提前终止（CANCELED）仍计入，
 * 只有退款（REFUNDED）才从收益里扣掉。
 */
export function getCreatorRevenue() {
  return api.get<ApiResponse<CreatorRevenue>>('/creator/revenue')
}
