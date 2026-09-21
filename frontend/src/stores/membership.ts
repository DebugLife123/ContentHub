import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { mySubscriptions } from '@/api/subscription'

/**
 * 会员状态。
 *
 * <p>内容库的解锁判断在后端（`ContentAccessService.decide()`），前端只是渲染
 * 后端下发的 `locked`。Skill 商城目前是 mock 数据、没有后端，所以这里用
 * 「有没有一条仍然有效的订阅」来判定会员身份，口径与后端一致：
 * `status = ACTIVE 且 endTime 还没过`，后端返回的 `valid` 字段就是这个判断。</p>
 *
 * <p>等 Skill 有了后端接口，解锁判断应该挪回服务端，这个 store 就只剩
 * 「展示会员到期时间」的作用。</p>
 */
export const useMembershipStore = defineStore('membership', () => {
  const loading = ref(false)
  const loaded = ref(false)
  /** 有效订阅里最晚的到期时间；为空表示不是会员 */
  const validUntil = ref('')

  const isMember = computed(() => !!validUntil.value)

  async function ensureLoaded(force = false) {
    if (loading.value) return
    if (loaded.value && !force) return
    loading.value = true
    try {
      const res = await mySubscriptions(1, 50)
      if (res.data.success) {
        validUntil.value = res.data.data.list
          .filter((s) => s.valid)
          .map((s) => s.endTime)
          .sort()
          .pop() ?? ''
      } else {
        validUntil.value = ''
      }
    } catch {
      // 未登录或后端不可达：当作非会员，页面走锁住的分支
      validUntil.value = ''
    } finally {
      loaded.value = true
      loading.value = false
    }
  }

  /** 退出登录时调用，否则会残留上一个账号的会员状态 */
  function reset() {
    loaded.value = false
    validUntil.value = ''
  }

  return { loading, loaded, validUntil, isMember, ensureLoaded, reset }
})
