import api from '../axios'
import type { AdminUser, ApiResponse, Comment, CreatorApplication, PageResult, SubscriptionPlan } from './types'

// ---------------------------------------------------------- 创作者申请审核

export function pageCreatorApplications(params: {
  status?: string
  pageNum?: number
  pageSize?: number
} = {}) {
  return api.get<ApiResponse<PageResult<CreatorApplication>>>('/admin/creator-applications', { params })
}

export function creatorApplicationPendingCount() {
  return api.get<ApiResponse<number>>('/admin/creator-applications/pending-count')
}

export function approveCreatorApplication(id: number) {
  return api.post<ApiResponse<void>>(`/admin/creator-applications/${id}/approve`)
}

export function rejectCreatorApplication(id: number, reason: string) {
  return api.post<ApiResponse<void>>(`/admin/creator-applications/${id}/reject`, { reason })
}

// ---------------------------------------------------------- 用户管理（阶段 6 Day 51）

export function pageAdminUsers(params: {
  keyword?: string
  role?: string
  pageNum?: number
  pageSize?: number
} = {}) {
  return api.get<ApiResponse<PageResult<AdminUser>>>('/admin/users', { params })
}

export function updateUserStatus(userId: number, status: 'ENABLED' | 'DISABLED') {
  return api.put<ApiResponse<void>>(`/admin/users/${userId}/status`, { status })
}

// ---------------------------------------------------------- 评论管理（阶段 6 Day 54）

export function pageAdminComments(params: {
  status?: string
  pageNum?: number
  pageSize?: number
} = {}) {
  return api.get<ApiResponse<PageResult<Comment>>>('/admin/comments', { params })
}

export function updateCommentStatus(commentId: number, status: 'NORMAL' | 'HIDDEN') {
  return api.put<ApiResponse<void>>(`/admin/comments/${commentId}/status`, { status })
}

export function deleteCommentAsAdmin(commentId: number) {
  return api.delete<ApiResponse<void>>(`/admin/comments/${commentId}`)
}

// ---------------------------------------------------------- 全平台套餐（阶段 6 Day 50）

export function listAllPlans() {
  return api.get<ApiResponse<SubscriptionPlan[]>>('/admin/plans')
}
