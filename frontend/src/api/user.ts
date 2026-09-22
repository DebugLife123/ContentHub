import api from '../axios'
import type { ApiResponse, UserInfo } from './types'

/** 个人资料与密码。用户名与角色不允许在这里改。 */

export function updateMyProfile(payload: {
  nickname?: string | null
  avatar?: string | null
  email?: string | null
  bio?: string | null
}) {
  return api.put<ApiResponse<UserInfo>>('/users/me', payload)
}

export function changeMyPassword(payload: { oldPassword: string; newPassword: string }) {
  return api.put<ApiResponse<void>>('/users/me/password', payload)
}
