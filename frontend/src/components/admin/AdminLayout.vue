<template>
  <div class="admin-shell">
    <!-- 进入控制台的加载过场（自己管自己的显隐，走完通知父级淡入内容） -->
    <AdminBoot @done="booting = false" />

    <!-- 移动端：抽屉遮罩 -->
    <div v-if="navOpen" class="admin-side-mask" @click="navOpen = false" />

    <!-- 侧边栏：各管理模块 -->
    <aside class="admin-side" :class="{ 'is-open': navOpen }">
      <div class="admin-brand">
        <RouterLink to="/" class="admin-brand-mark">
          <span class="brand-dot" />
          <span>Content<span>Hub</span></span>
        </RouterLink>
        <p class="admin-brand-sub">ADMIN CONSOLE</p>
      </div>

      <nav class="admin-nav">
        <template v-for="group in NAV" :key="group.label">
          <p class="admin-nav-group">{{ group.label }}</p>
          <RouterLink
            v-for="item in group.items"
            :key="item.to"
            :to="item.to"
            class="admin-nav-item"
            :class="{ 'is-active': current.to === item.to }"
            @click="navOpen = false"
          >
            <span class="admin-nav-no">{{ item.no }}</span>
            <span class="admin-nav-label">{{ item.label }}</span>
            <span v-if="badgeOf(item) > 0" class="admin-nav-badge">
              {{ badgeOf(item) > 99 ? '99+' : badgeOf(item) }}
            </span>
          </RouterLink>
        </template>
      </nav>

      <div class="admin-side-foot">
        <div class="admin-side-user">
          <span class="admin-avatar">{{ adminInitial }}</span>
          <div>
            <strong>{{ adminName }}</strong>
            <small>{{ roleLabel }}</small>
          </div>
        </div>
        <RouterLink to="/" class="admin-side-link">返回站点 <span>↗</span></RouterLink>
      </div>
    </aside>

    <!-- 主区 -->
    <div class="admin-body">
      <header class="admin-topbar">
        <div class="admin-crumb">
          <button class="admin-menu-toggle" type="button" @click="navOpen = true">☰ 模块</button>
          <span>管理后台</span>
          <span class="admin-crumb-sep">/</span>
          <span class="admin-crumb-current">{{ current.no }} · {{ current.label }}</span>
        </div>

        <div class="admin-topbar-actions">
          <span class="admin-topbar-note">{{ today }}</span>
        </div>
      </header>

      <main class="admin-main" :class="{ 'is-booting': booting }">
        <RouterView v-slot="{ Component }">
          <Transition name="admin-fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { pageForReview } from '@/api/content'
import { creatorApplicationPendingCount } from '@/api/admin'
import { useUserStore } from '@/stores/user'
import AdminBoot from './AdminBoot.vue'

/**
 * 管理后台外壳。
 *
 * 独立于站点头部/页脚：进入 /admin/* 后（见 App.vue 的 layout 分支）由本组件
 * 接管整屏，模块切换只换中间内容区，外壳不动。
 */
interface NavItem {
  to: string
  no: string
  label: string
  /** 需要在侧栏显示待处理数量徽标的统计口径 */
  badgeKey?: 'pendingContents' | 'pendingApplications'
}

const NAV: { label: string; items: NavItem[] }[] = [
  {
    label: 'OVERVIEW',
    items: [{ to: '/admin', no: '00', label: '控制台概览' }],
  },
  {
    label: 'CONTENT',
    items: [
      { to: '/admin/contents', no: '01', label: '内容审核', badgeKey: 'pendingContents' },
      { to: '/admin/creator-applications', no: '02', label: '创作者申请', badgeKey: 'pendingApplications' },
      { to: '/admin/comments', no: '03', label: '评论管理' },
      { to: '/admin/skill-comments', no: '04', label: 'Skill 评论' },
    ],
  },
  {
    label: 'SKILL MARKET',
    items: [{ to: '/admin/skills', no: '05', label: 'Skill 商城' }],
  },
  {
    label: 'PLATFORM',
    items: [
      { to: '/admin/users', no: '06', label: '用户管理' },
      { to: '/admin/categories', no: '07', label: '分类管理' },
      { to: '/admin/plans', no: '08', label: '套餐管理' },
    ],
  },
]

const route = useRoute()
const userStore = useUserStore()

const booting = ref(true)
const navOpen = ref(false)
const counts = ref({ pendingContents: 0, pendingApplications: 0 })

const allItems = NAV.flatMap((group) => group.items)

/** 侧栏徽标数量 */
function badgeOf(item: NavItem): number {
  return item.badgeKey ? counts.value[item.badgeKey] : 0
}

const current = computed(() => {
  // 子页面（如 /admin/skills/new）归属到最长的父级模块，保证侧栏始终有选中态
  const match = allItems
    .filter((item) => (item.to === '/admin' ? route.path === '/admin' : route.path.startsWith(item.to)))
    .sort((a, b) => b.to.length - a.to.length)[0]
  return match ?? { to: route.path, no: '—', label: String(route.meta.title || '管理后台') }
})

const adminName = computed(() =>
  userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员')
const adminInitial = computed(() => adminName.value.slice(0, 1).toUpperCase())
const roleLabel = computed(() => {
  const map: Record<string, string> = { USER: '普通用户', CREATOR: '创作者', ADMIN: '管理员' }
  return userStore.role ? map[userStore.role] ?? userStore.role : 'ADMIN'
})

const today = computed(() => {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}.${pad(now.getMonth() + 1)}.${pad(now.getDate())}`
})

/** 侧栏徽标：待审核内容 + 待处理创作者申请。失败就静默不显示，不打扰管理操作。 */
async function loadPending() {
  const [contents, applications] = await Promise.allSettled([
    pageForReview({ status: 'PENDING', pageNum: 1, pageSize: 1 }),
    creatorApplicationPendingCount(),
  ])

  counts.value.pendingContents =
    contents.status === 'fulfilled' && contents.value.data.success
      ? contents.value.data.data.total
      : 0
  counts.value.pendingApplications =
    applications.status === 'fulfilled' && applications.value.data.success
      ? applications.value.data.data
      : 0
}

onMounted(loadPending)

// 切换模块时回到顶部，并刷新待处理徽标（审核操作后能立刻看到数字下降）
watch(() => route.path, () => {
  navOpen.value = false
  window.scrollTo({ top: 0 })
  loadPending()
})
</script>
