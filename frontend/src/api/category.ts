import api from '../axios'
import type { ApiResponse, Category, CategoryPayload } from './types'

/** 分类列表（仅启用中，公开） */
export function listCategories() {
  return api.get<ApiResponse<Category[]>>('/categories')
}

/** 全部分类（含禁用，仅管理员） */
export function listAllCategories() {
  return api.get<ApiResponse<Category[]>>('/categories/all')
}

/** 新增分类（仅管理员） */
export function createCategory(payload: CategoryPayload) {
  return api.post<ApiResponse<Category>>('/categories', payload)
}

/** 修改分类（仅管理员） */
export function updateCategory(id: number, payload: CategoryPayload) {
  return api.put<ApiResponse<Category>>(`/categories/${id}`, payload)
}

/** 删除分类（仅管理员） */
export function deleteCategory(id: number) {
  return api.delete<ApiResponse<void>>(`/categories/${id}`)
}
