import api from '../axios'
import type {
  ApiResponse, Comment, ContentItem, ContentPayload, ContentQuery, PageResult, ReadingHistory,
} from './types'

/** 公开分页：仅已发布内容 */
export function pageContents(query: ContentQuery = {}) {
  return api.get<ApiResponse<PageResult<ContentItem>>>('/contents/page', { params: query })
}

/** 我的内容：含全部状态，需创作者或管理员 */
export function pageMyContents(query: ContentQuery = {}) {
  return api.get<ApiResponse<PageResult<ContentItem>>>('/contents/mine', { params: query })
}

/** 内容详情（仅已发布；无订阅权限时只拿到 bodyPreview） */
export function getContent(id: string | number) {
  return api.get<ApiResponse<ContentItem>>(`/contents/${id}`)
}

/** 我的内容详情：不限状态，编辑回显用 */
export function getMyContent(id: string | number) {
  return api.get<ApiResponse<ContentItem>>(`/contents/mine/${id}`)
}

export function createContent(payload: ContentPayload) {
  return api.post<ApiResponse<number>>('/contents', payload)
}

export function updateContent(id: string | number, payload: ContentPayload) {
  return api.put<ApiResponse<void>>(`/contents/${id}`, payload)
}

export function deleteContent(id: string | number) {
  return api.delete<ApiResponse<void>>(`/contents/${id}`)
}

// ---------------------------------------------------------------- 状态流转

/** 提交审核：草稿/已驳回/已下架 -> 待审核 */
export function submitContent(id: number) {
  return api.post<ApiResponse<void>>(`/contents/${id}/submit`)
}

/** 下架：已发布 -> 已下架 */
export function offlineContent(id: number) {
  return api.post<ApiResponse<void>>(`/contents/${id}/offline`)
}

// ------------------------------------------------------------------ 管理端

/** 管理端内容列表（默认待审核） */
export function pageForReview(query: ContentQuery = {}) {
  return api.get<ApiResponse<PageResult<ContentItem>>>('/admin/contents', { params: query })
}

export function approveContent(id: number) {
  return api.post<ApiResponse<void>>(`/admin/contents/${id}/approve`)
}

export function rejectContent(id: number, reason: string) {
  return api.post<ApiResponse<void>>(`/admin/contents/${id}/reject`, { reason })
}

// ------------------------------------------------------------------ 收藏

export function favoriteContent(id: number) {
  return api.post<ApiResponse<void>>(`/contents/${id}/favorite`)
}

export function unfavoriteContent(id: number) {
  return api.delete<ApiResponse<void>>(`/contents/${id}/favorite`)
}

export function myFavorites(pageNum = 1, pageSize = 9) {
  return api.get<ApiResponse<PageResult<ContentItem>>>('/users/me/favorites', {
    params: { pageNum, pageSize },
  })
}

// ------------------------------------------------- 阶段 5：热门 / 评论 / 阅读历史

/** 热门内容（Redis ZSet 排名） */
export function hotContents(limit = 6) {
  return api.get<ApiResponse<ContentItem[]>>('/contents/hot', { params: { limit } })
}

export function listComments(contentId: number, pageNum = 1, pageSize = 10) {
  return api.get<ApiResponse<PageResult<Comment>>>(
    `/contents/${contentId}/comments`, { params: { pageNum, pageSize } })
}

export function createComment(contentId: number, body: string) {
  return api.post<ApiResponse<Comment>>(`/contents/${contentId}/comments`, { body })
}

export function deleteComment(commentId: number) {
  return api.delete<ApiResponse<void>>(`/comments/${commentId}`)
}

/** 回传阅读进度，用于「最近阅读」的进度条 */
export function updateReadProgress(contentId: number, progress: number) {
  return api.put<ApiResponse<void>>(`/contents/${contentId}/progress`, { progress })
}

export function myHistory(pageNum = 1, pageSize = 10) {
  return api.get<ApiResponse<PageResult<ReadingHistory>>>('/users/me/history', {
    params: { pageNum, pageSize },
  })
}

export function myComments(pageNum = 1, pageSize = 10) {
  return api.get<ApiResponse<PageResult<Comment>>>('/users/me/comments', {
    params: { pageNum, pageSize },
  })
}
