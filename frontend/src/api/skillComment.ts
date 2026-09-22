import api from '../axios'
import type { ApiResponse, PageResult } from './types'

/**
 * Skill 评论。
 *
 * 内容库的评论接口在 api/content.ts 里，这里是另一条业务线的独立实现
 * （表也是分开的：comments 绑 content_id，skill_comment 绑 skill_id）。
 */
export interface SkillComment {
  id: number
  skillId: number
  skillName?: string | null
  userId: number
  username?: string | null
  status: 'NORMAL' | 'HIDDEN'
  body: string
  /** 当前登录用户能否删这条（作者本人或管理员） */
  canDelete?: boolean
  createTime?: string
}

export function listSkillComments(skillId: number | string, pageNum = 1, pageSize = 5) {
  return api.get<ApiResponse<PageResult<SkillComment>>>(`/skills/${skillId}/comments`, {
    params: { pageNum, pageSize },
  })
}

export function createSkillComment(skillId: number | string, body: string) {
  return api.post<ApiResponse<SkillComment>>(`/skills/${skillId}/comments`, { body })
}

export function deleteSkillComment(commentId: number) {
  return api.delete<ApiResponse<void>>(`/skill-comments/${commentId}`)
}

export function mySkillComments(pageNum = 1, pageSize = 10) {
  return api.get<ApiResponse<PageResult<SkillComment>>>('/skill-comments/mine', {
    params: { pageNum, pageSize },
  })
}

// ---------------------------------------------------------------- 管理端

export function pageAdminSkillComments(params: {
  status?: string
  pageNum?: number
  pageSize?: number
} = {}) {
  return api.get<ApiResponse<PageResult<SkillComment>>>('/admin/skill-comments', { params })
}

export function updateSkillCommentStatus(commentId: number, status: 'NORMAL' | 'HIDDEN') {
  return api.put<ApiResponse<void>>(`/admin/skill-comments/${commentId}/status`, { status })
}

export function deleteSkillCommentAsAdmin(commentId: number) {
  return api.delete<ApiResponse<void>>(`/admin/skill-comments/${commentId}`)
}
