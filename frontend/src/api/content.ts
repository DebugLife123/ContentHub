import api from '../axios'
import type { ApiResponse, ContentItem, ContentPayload, ContentQuery, PageResult } from './types'

/** 公开分页：仅已发布内容（计划表 19：GET /api/contents/page） */
export function pageContents(query: ContentQuery = {}) {
  return api.get<ApiResponse<PageResult<ContentItem>>>('/contents/page', { params: query })
}

/** 我的内容：含草稿与下架，需创作者或管理员 */
export function pageMyContents(query: ContentQuery = {}) {
  return api.get<ApiResponse<PageResult<ContentItem>>>('/contents/mine', { params: query })
}

/** 内容详情（仅已发布） */
export function getContent(id: string | number) {
  return api.get<ApiResponse<ContentItem>>(`/contents/${id}`)
}

/** 我的内容详情：不限状态，用于编辑草稿时回显 */
export function getMyContent(id: string | number) {
  return api.get<ApiResponse<ContentItem>>(`/contents/mine/${id}`)
}

/** 新增内容 */
export function createContent(payload: ContentPayload) {
  return api.post<ApiResponse<number>>('/contents', payload)
}

/** 编辑内容 */
export function updateContent(id: string | number, payload: ContentPayload) {
  return api.put<ApiResponse<void>>(`/contents/${id}`, payload)
}

/** 删除内容（逻辑删除） */
export function deleteContent(id: string | number) {
  return api.delete<ApiResponse<void>>(`/contents/${id}`)
}
