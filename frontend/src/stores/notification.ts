import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  markAllNotificationsRead,
  markNotificationRead,
  unreadNotificationCount,
} from '@/api/notification'

/**
 * 未读通知数。
 *
 * <p>抽成 store 是因为这个数字被两处用：顶栏的铃铛角标、通知页的「未读 N 条」。
 * 之前角标是 AppHeader 的局部 ref，通知页读完只更新自己，
 * 结果必须刷新页面角标才会消失——这正是要修的问题。</p>
 *
 * <p>所有会改变未读数的操作都收敛到这里，调用方不再自己维护计数。</p>
 */
export const useNotificationStore = defineStore('notification', () => {
  const unread = ref(0)

  /** 从服务端拉一次真实值（登录后、进通知页时用） */
  async function refresh() {
    try {
      const res = await unreadNotificationCount()
      unread.value = res.data.success ? res.data.data ?? 0 : 0
    } catch {
      // 未登录或后端不可达：角标不显示，而不是留个错的数字
      unread.value = 0
    }
  }

  /** 本地先减，让角标立刻响应，不用等下一次 refresh */
  function decrement(count = 1) {
    unread.value = Math.max(0, unread.value - count)
  }

  function clear() {
    unread.value = 0
  }

  /** 标记单条已读：接口成功后再减，失败就不动 */
  async function readOne(id: number) {
    await markNotificationRead(id)
    decrement()
  }

  /** 全部已读 */
  async function readAll() {
    await markAllNotificationsRead()
    clear()
  }

  return { unread, refresh, decrement, clear, readOne, readAll }
})
