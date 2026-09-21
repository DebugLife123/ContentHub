import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import { TOKEN_KEY } from '@/axios'
import { useMembershipStore } from './membership'
import type { Role, UserInfo } from '@/api/types'

/**
 * 登录态（计划 Day 18：Pinia、路由守卫、用户状态）。
 *
 * token 存 localStorage（由 axios 拦截器自动附加到请求头），
 * 用户信息在需要时按需拉取。
 */
export const useUserStore = defineStore(
  'user',
  () => {
    const token = ref<string>('')
    const userInfo = ref<UserInfo | null>(null)

    const isLoggedIn = computed(() => !!token.value)
    const role = computed<Role | null>(() => userInfo.value?.role ?? null)

    /** 登录：拿到 token 后立刻拉一次用户信息，供路由守卫判断角色 */
    async function login(username: string, password: string) {
      const res = await authApi.login(username, password)
      if (!res.data.success) {
        throw new Error(res.data.message || '登录失败')
      }
      token.value = res.data.data.token
      localStorage.setItem(TOKEN_KEY, token.value)
      await fetchCurrentUser()
      return userInfo.value
    }

    /** 拉取当前用户信息；token 已失效时清空登录态 */
    async function fetchCurrentUser() {
      if (!token.value) {
        userInfo.value = null
        return null
      }
      try {
        const res = await authApi.getCurrentUser()
        if (res.data.success) {
          userInfo.value = res.data.data
        } else {
          clear()
        }
      } catch {
        clear()
      }
      return userInfo.value
    }

    /** 退出登录：后端删除 Redis 中的 token，前端清空本地状态 */
    async function logout() {
      try {
        if (token.value) {
          await authApi.logout()
        }
      } catch {
        // 后端不可达也要让本地退出，否则用户会卡在登录态出不去
      } finally {
        clear()
      }
    }

    function clear() {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem(TOKEN_KEY)
    // 会员状态是按当前账号查出来的，退出时必须一起清掉
    useMembershipStore().reset()
    }

    /** 角色判断：管理员同时具备创作者能力（与后端 Security 规则一致） */
    function hasRole(...roles: Role[]) {
      return !!userInfo.value && roles.includes(userInfo.value.role)
    }

    return { token, userInfo, isLoggedIn, role, login, logout, fetchCurrentUser, clear, hasRole }
  },
  {
    // 只持久化 token，刷新后由它重新拉取用户信息
    persist: {
      key: 'contenthub-user',
      paths: ['token'],
    },
  }
)
