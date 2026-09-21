import api from '../axios'
import type { ApiResponse, PlanPayload, SubscriptionPlan } from './types'

/** 已上架套餐（公开，阶段 4 Day 30） */
export function listPlans() {
  return api.get<ApiResponse<SubscriptionPlan[]>>('/plans')
}

/** 我的套餐（含已下架） */
export function listMyPlans() {
  return api.get<ApiResponse<SubscriptionPlan[]>>('/plans/mine')
}

export function createPlan(payload: PlanPayload) {
  return api.post<ApiResponse<SubscriptionPlan>>('/plans', payload)
}

export function updatePlan(id: number, payload: PlanPayload) {
  return api.put<ApiResponse<SubscriptionPlan>>(`/plans/${id}`, payload)
}

export function deletePlan(id: number) {
  return api.delete<ApiResponse<void>>(`/plans/${id}`)
}
