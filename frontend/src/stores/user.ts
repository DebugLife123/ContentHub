import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo } from '@/api/admin/user'
import { removeToken } from '@/composables/auth'

/** 当前登录用户信息 */
export interface UserInfo {
  username?: string
  nickname?: string
  avatar?: string
  /** USER / CREATOR / ADMIN */
  role?: string
}

export const useUserStore = defineStore(
  'user',
  () => {
    // 用户信息
    const userInfo = ref<UserInfo>({})

    // 设置用户信息：调用后端接口拉取
    function setUserInfo() {
      getUserInfo().then((res) => {
        if (res.data.success) {
          userInfo.value = res.data.data as UserInfo
        }
      })
    }

    // 退出登录
    function logout() {
      // 删除 cookie 中的令牌
      removeToken()
      // 删除登录用户的信息
      userInfo.value = {}
    }

    return { userInfo, setUserInfo, logout }
  },
  {
    // 开启持久化
    persist: true,
  }
)
