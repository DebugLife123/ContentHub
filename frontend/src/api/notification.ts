import api from '../axios'
import type { ApiResponse, PageResult } from './types'

/**
 * 站内通知。
 *
 * 存在的意义是把「审核结果」主动告诉当事人——改造前管理员驳回内容只会写进
 * `reject_reason`，创作者不去工作台翻就永远不知道。
 */
export interface NotificationItem {
  id: number
  /** CONTENT_APPROVED / CONTENT_REJECTED / CONTENT_OFFLINE / CREATOR_APPROVED / CREATOR_REJECTED */
  type: string
  title: string
  body?: string | null
  /** CONTENT / CREATOR_APPLICATION */
  bizType?: string | null
  bizId?: number | null
  read: boolean
  createTime?: string
}

export function myNotifications(params: { pageNum?: number; pageSize?: number; unreadOnly?: boolean } = {}) {
  return api.get<ApiResponse<PageResult<NotificationItem>>>('/notifications', { params })
}

export function unreadNotificationCount() {
  return api.get<ApiResponse<number>>('/notifications/unread-count')
}

export function markNotificationRead(id: number) {
  return api.post<ApiResponse<void>>(`/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return api.post<ApiResponse<void>>('/notifications/read-all')
}
