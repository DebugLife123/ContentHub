<template>
  <header class="topbar">
    <div class="topbar-inner">
      <RouterLink to="/" class="brand-mark">
        <span class="brand-dot"></span>
        <span>Content<span>Hub</span></span>
      </RouterLink>

      <!-- 计划 Day 55：不同角色看到不同菜单 -->
      <nav class="main-nav">
        <RouterLink to="/">发现内容</RouterLink>
        <RouterLink to="/contents">内容库</RouterLink>
        <RouterLink to="/plans">订阅方案</RouterLink>
        <RouterLink to="/skills">Skill 商城</RouterLink>
        <RouterLink v-if="canCreate" to="/creator">创作者工作台</RouterLink>

        <!-- 管理后台已是独立整屏控制台，这里只留一个入口，模块导航交给它的侧边栏 -->
        <RouterLink v-if="isAdmin" to="/admin" class="admin-entry">
          管理后台 <i>↗</i>
        </RouterLink>
      </nav>

      <div class="header-actions">
        <template v-if="userStore.isLoggedIn">
          <RouterLink
            to="/notifications"
            class="bell-link"
            :title="unread > 0 ? `${unread} 条未读通知` : '通知'"
          >
            <svg class="bell-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
              <path d="M13.7 21a2 2 0 0 1-3.4 0" />
            </svg>
            <span v-if="unread > 0" class="bell-badge">{{ unread > 99 ? '99+' : unread }}</span>
            <span class="sr-only">站内通知</span>
          </RouterLink>

          <!--
            个人中心入口。
            以前这里只是一串纯文字（昵称 + 角色标签），既没有头像也没有图标，
            在一排导航里几乎没有"可以点"的暗示。现在做成「头像 + 昵称 + 角色」
            的按钮，点开是一个用户菜单。
          -->
          <div ref="menuRoot" class="user-menu">
            <button
              type="button"
              class="user-trigger"
              :class="{ 'is-open': menuOpen }"
              :aria-expanded="menuOpen"
              aria-haspopup="menu"
              @click="toggleMenu"
            >
              <span class="user-avatar" aria-hidden="true">{{ avatarText }}</span>
              <span class="user-name">{{ displayName }}</span>
              <span class="user-role">{{ roleLabel }}</span>
              <svg class="chevron" viewBox="0 0 24 24" aria-hidden="true">
                <path d="m6 9 6 6 6-6" />
              </svg>
            </button>

            <Transition name="menu-pop">
              <div v-if="menuOpen" class="user-dropdown" role="menu">
                <div class="menu-head">
                  <span class="menu-avatar" aria-hidden="true">{{ avatarText }}</span>
                  <div class="menu-identity">
                    <strong>{{ displayName }}</strong>
                    <small>{{ userStore.userInfo?.email || roleLabel }}</small>
                  </div>
                </div>

                <RouterLink to="/profile" class="menu-item" role="menuitem" @click="closeMenu">
                  个人中心
                </RouterLink>
                <RouterLink to="/subscriptions" class="menu-item" role="menuitem" @click="closeMenu">
                  我的订阅
                </RouterLink>
                <RouterLink v-if="canCreate" to="/creator" class="menu-item" role="menuitem" @click="closeMenu">
                  创作者工作台
                </RouterLink>
                <RouterLink v-if="isAdmin" to="/admin" class="menu-item" role="menuitem" @click="closeMenu">
                  管理后台
                </RouterLink>

                <div class="menu-divider"></div>
                <button type="button" class="menu-item is-danger" role="menuitem" @click="handleLogout">
                  退出登录
                </button>
              </div>
            </Transition>
          </div>
        </template>

        <template v-else>
          <RouterLink to="/login" class="login-link">登录</RouterLink>
          <RouterLink to="/register" class="creator-cta">注册 <span>↗</span></RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'

const userStore = useUserStore()
const notificationStore = useNotificationStore()
const router = useRouter()
const route = useRoute()

const menuRoot = ref<HTMLElement | null>(null)
const menuOpen = ref(false)

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}
function closeMenu() {
  menuOpen.value = false
}

/** 点空白处 / 按 Esc 收起菜单 */
function onDocumentClick(event: MouseEvent) {
  if (!menuOpen.value) return
  if (menuRoot.value && !menuRoot.value.contains(event.target as Node)) closeMenu()
}
function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') closeMenu()
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('keydown', onKeydown)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('keydown', onKeydown)
})

// 跳转后收起菜单，避免下一页还挂着浮层
watch(() => route.fullPath, closeMenu)

/** 未读通知数来自 store：通知页读完之后角标会立刻跟着变，不用刷新页面 */
const unread = computed(() => notificationStore.unread)

// 登录/退出后立刻刷新角标，不用等下一次进页面
watch(
  () => userStore.isLoggedIn,
  () => notificationStore.refresh(),
)

/** 管理员同时具备创作者能力，与后端 Security 规则保持一致 */
const canCreate = computed(() => userStore.hasRole('CREATOR', 'ADMIN'))
const isAdmin = computed(() => userStore.hasRole('ADMIN'))

const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '我的',
)

/** 没有真实头像时的兜底：取昵称首字，中英文都能用 */
const avatarText = computed(() => (displayName.value || '?').trim().charAt(0).toUpperCase())

const roleLabel = computed(() => {
  const map: Record<string, string> = { USER: '普通用户', CREATOR: '创作者', ADMIN: '管理员' }
  return userStore.role ? map[userStore.role] ?? userStore.role : ''
})

async function handleLogout() {
  closeMenu()
  await userStore.logout()
  router.push('/')
}

// 刷新页面后补齐用户信息（路由守卫在跳转时也会做一次）
onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    await userStore.fetchCurrentUser()
  }
  await notificationStore.refresh()
})
</script>

<style scoped>
.topbar-inner {
  gap: 24px;
}

/* ------------------------------------------------------------------ 导航
   导航项是中文，必须走 UI 字体栈；用等宽字体时中文会回落到系统等宽字形，
   看起来又小又细。这里顺带把字号从 12px 提到 14px。 */
.main-nav {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-left: 40px;
  flex: 1;
}

.main-nav a {
  position: relative;
  font: 500 14px var(--font-ui);
  letter-spacing: 0;
  color: var(--muted);
  padding: 4px 2px;
  transition: color var(--dur-base) var(--ease-out);
}

/* 下划线用伪元素做，hover 时从左往右展开 */
.main-nav a:not(.admin-entry)::after {
  content: '';
  position: absolute;
  left: 2px;
  right: 2px;
  bottom: -2px;
  height: 1px;
  background: var(--ink);
  transform: scaleX(0);
  transform-origin: 0 50%;
  transition: transform var(--dur-base) var(--ease-out);
}

.main-nav a:not(.admin-entry):hover::after,
.main-nav a.router-link-active:not(.admin-entry)::after {
  transform: scaleX(1);
}

.main-nav a:hover,
.main-nav .router-link-active {
  color: var(--ink);
}

/* 管理后台入口：比普通导航多一层描边，明确它是"另一个工作区"的入口 */
.admin-entry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 13px;
  border: 1px solid var(--ink);
  font: 500 12.5px var(--font-ui);
  letter-spacing: 0.02em;
  color: var(--ink);
  text-decoration: none;
  transition: background var(--dur-base) var(--ease-out), color var(--dur-base) var(--ease-out);
}

.admin-entry i {
  font-style: normal;
  color: var(--orange);
  transition: color var(--dur-base) var(--ease-out);
}

.admin-entry:hover {
  background: var(--ink);
  color: #fff;
}

.admin-entry:hover i {
  color: var(--orange);
}

/* ------------------------------------------------------------------ 通知铃 */
.bell-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  text-decoration: none;
  color: var(--muted);
  transition: background var(--dur-base) var(--ease-out), color var(--dur-base) var(--ease-out),
    transform var(--dur-base) var(--ease-out);
}

.bell-link:hover {
  background: rgba(23, 23, 23, 0.06);
  color: var(--ink);
  transform: translateY(-1px);
}

.bell-icon {
  width: 19px;
  height: 19px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.6;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.bell-badge {
  position: absolute;
  top: 0;
  right: 0;
  min-width: 17px;
  height: 17px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--orange);
  color: #fff;
  font: 500 10.5px var(--font-mono);
  line-height: 17px;
  text-align: center;
  box-shadow: 0 0 0 2px var(--paper);
}

/* ---------------------------------------------------------------- 用户菜单 */
.user-menu {
  position: relative;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  height: 38px;
  padding: 0 12px 0 5px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: none;
  cursor: pointer;
  font-family: inherit;
  transition: background var(--dur-base) var(--ease-out), border-color var(--dur-base) var(--ease-out);
}

.user-trigger:hover,
.user-trigger.is-open {
  background: rgba(23, 23, 23, 0.05);
  border-color: var(--line);
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--ink);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font: 600 13px var(--font-ui);
  flex: none;
}

.user-name {
  font: 500 14px var(--font-ui);
  color: var(--ink);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role {
  padding: 2px 7px;
  border: 1px solid var(--line);
  border-radius: 3px;
  font: 500 11px var(--font-ui);
  color: var(--muted);
  white-space: nowrap;
}

.chevron {
  width: 15px;
  height: 15px;
  fill: none;
  stroke: var(--muted);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
  transition: transform var(--dur-base) var(--ease-out);
}

.user-trigger.is-open .chevron {
  transform: rotate(180deg);
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 232px;
  padding: 6px;
  background: #fff;
  border: 1px solid var(--line);
  box-shadow: 0 18px 40px -18px rgba(23, 23, 23, 0.35);
  z-index: 60;
}

.menu-head {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 10px 10px 12px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 6px;
}

.menu-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--ink);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font: 600 15px var(--font-ui);
  flex: none;
}

.menu-identity {
  min-width: 0;
}

.menu-identity strong {
  display: block;
  font: 600 14px var(--font-ui);
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-identity small {
  display: block;
  font: 400 12px var(--font-ui);
  color: var(--muted);
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-item {
  display: block;
  width: 100%;
  padding: 9px 10px;
  border: 0;
  border-radius: 3px;
  background: none;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
  font: 500 13.5px var(--font-ui);
  color: var(--ink);
  transition: background var(--dur-fast) var(--ease-out);
}

.menu-item:hover {
  background: var(--art-surface, #edeae2);
}

.menu-item.is-danger {
  color: #a33b2a;
}

.menu-divider {
  height: 1px;
  background: var(--line);
  margin: 6px 4px;
}

/* 菜单展开动画 */
.menu-pop-enter-active,
.menu-pop-leave-active {
  transition: opacity var(--dur-fast) var(--ease-out), transform var(--dur-fast) var(--ease-out);
}

.menu-pop-enter-from,
.menu-pop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ------------------------------------------------------------- 未登录状态 */
.login-link {
  font: 500 14px var(--font-ui);
  color: var(--muted);
  text-decoration: none;
  padding: 4px 2px;
  transition: color var(--dur-base) var(--ease-out);
}

.login-link:hover {
  color: var(--ink);
}

.creator-cta {
  display: inline-flex;
  align-items: center;
  border: 0;
  cursor: pointer;
  background: var(--ink);
  color: #fff;
  text-decoration: none;
  padding: 11px 17px;
  font: 500 13px var(--font-ui);
  transition: background var(--dur-base) var(--ease-out), transform var(--dur-base) var(--ease-out);
}

/* 箭头在中文按钮里用橙色点一下，和全站的橙色点缀呼应 */
.creator-cta span {
  margin-left: 14px;
  color: var(--orange);
}

.creator-cta:hover {
  transform: translateY(-1px);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 800px) {
  .user-name,
  .user-role {
    display: none;
  }

  .user-trigger {
    padding: 0 6px;
  }
}
</style>
