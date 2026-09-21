<template>
  <div class="profile-page content-width">
    <div class="section-heading">
      <div><p class="eyebrow">MY ACCOUNT</p><h2>个人中心<br><em>{{ displayName }}</em></h2></div>
      <p class="heading-aside">
        角色：<strong>{{ roleLabel }}</strong><br>
        {{ roleHint }}
      </p>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <div v-else-if="!userInfo" class="empty-state">登录状态已失效，请重新登录。</div>
    <div v-else class="profile-grid">
      <section class="profile-panel">
        <h3>账号信息</h3>
        <dl class="info-list">
          <div><dt>用户名</dt><dd>{{ userInfo.username }}</dd></div>
          <div><dt>昵称</dt><dd>{{ userInfo.nickname || '未设置' }}</dd></div>
          <div><dt>邮箱</dt><dd>{{ userInfo.email || '未设置' }}</dd></div>
          <div><dt>角色</dt><dd>{{ roleLabel }}</dd></div>
          <div><dt>个人简介</dt><dd>{{ userInfo.bio || '这个人很懒，什么都没写。' }}</dd></div>
        </dl>
        <el-button class="button button-dark" @click="handleLogout">退出登录 <span>↗</span></el-button>
      </section>

      <section class="profile-panel">
        <h3>可以做什么</h3>
        <ul class="ability-list">
          <li v-for="item in abilities" :key="item.text" :class="{ disabled: !item.enabled }">
            <span>{{ item.enabled ? '✓' : '·' }}</span>
            <div>
              <strong>{{ item.text }}</strong>
              <small>{{ item.hint }}</small>
            </div>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)

const userInfo = computed(() => userStore.userInfo)
const displayName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '访客')

const roleLabel = computed(() => {
  const map: Record<string, string> = { USER: '普通用户', CREATOR: '创作者', ADMIN: '管理员' }
  return userStore.role ? map[userStore.role] ?? userStore.role : '未登录'
})

const roleHint = computed(() => {
  if (userStore.hasRole('ADMIN')) return '拥有平台全部管理权限'
  if (userStore.hasRole('CREATOR')) return '可以发布与管理自己的内容'
  return '可以浏览免费内容与互动'
})

const abilities = computed(() => [
  { text: '浏览免费内容', enabled: true, hint: '所有访客都可以' },
  { text: '发布与编辑内容', enabled: userStore.hasRole('CREATOR', 'ADMIN'), hint: '需要创作者身份' },
  { text: '管理内容分类', enabled: userStore.hasRole('ADMIN'), hint: '需要管理员身份' },
  { text: '订阅与收藏', enabled: false, hint: '计划阶段 4 / 5 实现' },
])

async function handleLogout() {
  await userStore.logout()
  router.push('/')
}

onMounted(async () => {
  // 路由守卫已确保登录，这里只负责把资料拉全
  await userStore.fetchCurrentUser()
  loading.value = false
})
</script>

<style scoped>
.profile-page {
  padding: 70px 0 30px;
}
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-top: 34px;
}
.profile-panel {
  border: 1px solid var(--line);
  padding: 26px;
}
.profile-panel h3 {
  font-size: 20px;
  letter-spacing: -0.04em;
  margin: 0 0 20px;
}
.info-list {
  margin: 0 0 24px;
}
.info-list > div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
}
.info-list dt {
  color: var(--muted);
  font: 11px 'DM Mono', monospace;
}
.info-list dd {
  margin: 0;
  text-align: right;
}
.ability-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.ability-list li {
  display: flex;
  gap: 12px;
  padding: 13px 0;
  border-bottom: 1px solid var(--line);
}
.ability-list li.disabled {
  color: var(--muted);
}
.ability-list li span {
  width: 16px;
}
.ability-list small {
  display: block;
  color: var(--muted);
  font: 10px 'DM Mono', monospace;
  margin-top: 5px;
}
@media (max-width: 800px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
