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
        <RouterLink v-if="canCreate" to="/creator">创作者工作台</RouterLink>

        <el-dropdown v-if="isAdmin" trigger="hover" @command="go">
          <span class="nav-dropdown">管理后台 <i>⌄</i></span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="/admin/contents">内容审核</el-dropdown-item>
              <el-dropdown-item command="/admin/comments">评论管理</el-dropdown-item>
              <el-dropdown-item command="/admin/users">用户管理</el-dropdown-item>
              <el-dropdown-item command="/admin/categories">分类管理</el-dropdown-item>
              <el-dropdown-item command="/admin/plans">套餐管理</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </nav>

      <div class="header-actions">
        <template v-if="userStore.isLoggedIn">
          <RouterLink to="/profile" class="login-link">
            {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '我的' }}
            <span class="role-badge">{{ roleLabel }}</span>
          </RouterLink>
          <button class="creator-cta" type="button" @click="handleLogout">退出 <span>↗</span></button>
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
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const router = useRouter()

/** 管理员同时具备创作者能力，与后端 Security 规则保持一致 */
const canCreate = computed(() => userStore.hasRole('CREATOR', 'ADMIN'))
const isAdmin = computed(() => userStore.hasRole('ADMIN'))

const roleLabel = computed(() => {
  const map: Record<string, string> = { USER: '普通用户', CREATOR: '创作者', ADMIN: '管理员' }
  return userStore.role ? map[userStore.role] ?? userStore.role : ''
})

function go(path: string) {
  router.push(path)
}

async function handleLogout() {
  await userStore.logout()
  router.push('/')
}

// 刷新页面后补齐用户信息（路由守卫在跳转时也会做一次）
onMounted(() => {
  if (userStore.token && !userStore.userInfo) {
    userStore.fetchCurrentUser()
  }
})
</script>

<style scoped>
.role-badge {
  margin-left: 6px;
  padding: 1px 6px;
  border: 1px solid var(--line);
  font-size: 9px;
  color: var(--muted);
}
.creator-cta {
  border: 0;
  cursor: pointer;
  font-family: inherit;
}
.main-nav {
  gap: 18px;
}
.nav-dropdown {
  font: 500 12px 'DM Mono', monospace;
  letter-spacing: 0.02em;
  color: var(--muted);
  cursor: pointer;
  outline: none;
}
.nav-dropdown:hover {
  color: var(--ink);
}
.nav-dropdown i {
  font-style: normal;
}
</style>
