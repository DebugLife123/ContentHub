import api from '../axios'
import type {
  ApiResponse,
  PageResult,
  SkillCategory,
  SkillCategoryPayload,
  SkillDetail,
  SkillItem,
  SkillPayload,
  SkillQuery,
} from './types'

/**
 * Skill 商城接口。
 *
 * <p>会员解锁判定在后端（`SkillServiceImpl.decideAccess`）：未解锁时详情接口
 * 不下发安装命令与快速上手步骤，前端只负责渲染 `locked`。所以这里没有
 * 「前端算会员状态」这回事——和内容库是同一套做法。</p>
 */

// ---------------------------------------------------------------- 公开读

/** Skill 分页（仅已上架，默认按星数倒序） */
export function pageSkills(query: SkillQuery = {}) {
  return api.get<ApiResponse<PageResult<SkillItem>>>('/skills', { params: query })
}

export function getSkill(id: number | string) {
  return api.get<ApiResponse<SkillDetail>>(`/skills/${id}`)
}

/** Skill 分类（仅启用中） */
export function listSkillCategories() {
  return api.get<ApiResponse<SkillCategory[]>>('/skill-categories')
}

// ---------------------------------------------------------------- 管理端

export function pageSkillsForAdmin(query: SkillQuery = {}) {
  return api.get<ApiResponse<PageResult<SkillItem>>>('/admin/skills', { params: query })
}

export function getSkillForAdmin(id: number | string) {
  return api.get<ApiResponse<SkillDetail>>(`/admin/skills/${id}`)
}

/** 新增；返回新建（或复活）的 id，落库为草稿 */
export function createSkill(payload: SkillPayload) {
  return api.post<ApiResponse<number>>('/admin/skills', payload)
}

export function updateSkill(id: number, payload: SkillPayload) {
  return api.put<ApiResponse<void>>(`/admin/skills/${id}`, payload)
}

export function deleteSkill(id: number) {
  return api.delete<ApiResponse<void>>(`/admin/skills/${id}`)
}

/** 上架：草稿 / 已下架 -> 已上架 */
export function publishSkill(id: number) {
  return api.post<ApiResponse<void>>(`/admin/skills/${id}/publish`)
}

/** 下架：已上架 -> 已下架 */
export function offlineSkill(id: number) {
  return api.post<ApiResponse<void>>(`/admin/skills/${id}/offline`)
}

// ---------------------------------------------------------------- 管理端分类

export function listAllSkillCategories() {
  return api.get<ApiResponse<SkillCategory[]>>('/admin/skill-categories')
}

export function createSkillCategory(payload: SkillCategoryPayload) {
  return api.post<ApiResponse<SkillCategory>>('/admin/skill-categories', payload)
}

export function updateSkillCategory(id: number, payload: SkillCategoryPayload) {
  return api.put<ApiResponse<SkillCategory>>(`/admin/skill-categories/${id}`, payload)
}

export function deleteSkillCategory(id: number) {
  return api.delete<ApiResponse<void>>(`/admin/skill-categories/${id}`)
}

/** 星数展示：过万折成 78.2k */
export function formatStars(stars: number): string {
  if (stars >= 1000) {
    return `${(stars / 1000).toFixed(1)}k`
  }
  return String(stars ?? 0)
}

/** 后端返回 yyyy-MM-dd HH:mm:ss，列表与侧栏只展示日期 */
export function formatDate(value?: string | null): string {
  if (!value) return '—'
  return value.slice(0, 10)
}
