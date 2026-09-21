import api from '../axios'
import type { ApiResponse, CreatorDashboard, CreatorProfile } from './types'

/** 申请成为创作者（普通用户 -> 创作者） */
export function applyCreator() {
  return api.post<ApiResponse<CreatorProfile>>('/creator/apply')
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
