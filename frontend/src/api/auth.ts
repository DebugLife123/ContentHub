import { instance, NotInterceptorInstance } from '@/axios'
import type { ApiResponse, LoginResult, UserInfo } from './types'

/**
 * 登录（计划表 19：POST /api/auth/login）
 *
 * 用 NotInterceptorInstance：登录时不该带上旧 token。
 */
export function login(username: string, password: string) {
  return NotInterceptorInstance.post<ApiResponse<LoginResult>>('/auth/login', { username, password })
}

/** 注册（POST /api/auth/register） */
export function register(username: string, password: string, confirmPassword: string) {
  return NotInterceptorInstance.post<ApiResponse<void>>('/auth/register', {
    username,
    password,
    confirmPassword,
  })
}

/**
 * 退出登录（POST /api/auth/logout）
 *
 * 后端会从 Redis 删除 token，使其立即失效——不是只在前端丢掉而已。
 */
export function logout() {
  return instance.post<ApiResponse<void>>('/auth/logout')
}

/** 当前登录用户（GET /api/users/me） */
export function getCurrentUser() {
  return instance.get<ApiResponse<UserInfo>>('/users/me')
}
