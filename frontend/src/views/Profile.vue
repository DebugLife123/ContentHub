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
    <template v-else>
      <div class="profile-grid">
        <section class="profile-panel">
          <h3>账号信息</h3>
          <dl class="info-list">
            <div><dt>用户名</dt><dd>{{ userInfo.username }}</dd></div>
            <div><dt>昵称</dt><dd>{{ userInfo.nickname || '未设置' }}</dd></div>
            <div><dt>邮箱</dt><dd>{{ userInfo.email || '未设置' }}</dd></div>
            <div><dt>角色</dt><dd>{{ roleLabel }}</dd></div>
          </dl>
          <div class="panel-actions">
            <el-button v-if="!isCreator" class="button button-dark" :loading="applying" @click="apply">
              申请成为创作者 <span>↗</span>
            </el-button>
            <el-button v-else class="button button-dark" @click="$router.push('/creator')">
              进入创作者工作台 <span>↗</span>
            </el-button>
            <el-button @click="$router.push('/subscriptions')">我的订阅</el-button>
            <el-button @click="handleLogout">退出登录</el-button>
          </div>
          <p v-if="applyMessage" class="success-text">{{ applyMessage }}</p>
        </section>

        <section class="profile-panel">
          <h3>可以做什么</h3>
          <ul class="ability-list">
            <li v-for="item in abilities" :key="item.text" :class="{ disabled: !item.enabled }">
              <span>{{ item.enabled ? '✓' : '·' }}</span>
              <div><strong>{{ item.text }}</strong><small>{{ item.hint }}</small></div>
            </li>
          </ul>
          <div class="sub-summary">
            <p class="eyebrow">SUBSCRIPTION</p>
            <p v-if="activeSubCount > 0">
              当前有 <strong>{{ activeSubCount }}</strong> 个生效中的订阅，可阅读对应创作者的订阅专属内容。
            </p>
            <p v-else>还没有生效中的订阅，付费内容只能看到试读片段。</p>
          </div>
        </section>
      </div>

      <section class="favorites">
        <div class="panel-head">
          <h3>我的收藏</h3>
          <span>{{ favoriteTotal }} 份</span>
        </div>
        <div v-if="!favorites.length" class="empty-state">还没有收藏任何内容。</div>
        <div v-else class="fav-grid">
          <article v-for="item in favorites" :key="item.id" class="fav-card"
                   @click="$router.push(`/content/${item.id}`)">
            <span class="fav-type">{{ item.contentType }}</span>
            <h4>{{ item.title }}</h4>
            <p>{{ item.summary || '一份正在持续更新的数字内容。' }}</p>
            <div class="fav-foot">
              <span>{{ item.accessType === 'FREE' ? '免费' : '订阅' }}</span>
              <span>{{ item.viewCount || 0 }} 次阅读</span>
            </div>
          </article>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { applyCreator } from '@/api/creator'
import { myFavorites } from '@/api/content'
import { mySubscriptions } from '@/api/subscription'
import { useUserStore } from '@/stores/user'
import type { ContentItem } from '@/api/types'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const applying = ref(false)
const applyMessage = ref('')
const favorites = ref<ContentItem[]>([])
const favoriteTotal = ref(0)
const activeSubCount = ref(0)

const userInfo = computed(() => userStore.userInfo)
const displayName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '访客')
const isCreator = computed(() => userStore.hasRole('CREATOR', 'ADMIN'))

const roleLabel = computed(() => {
  const map: Record<string, string> = { USER: '普通用户', CREATOR: '创作者', ADMIN: '管理员' }
  return userStore.role ? map[userStore.role] ?? userStore.role : '未登录'
})

const roleHint = computed(() => {
  if (userStore.hasRole('ADMIN')) return '拥有平台全部管理权限'
  if (userStore.hasRole('CREATOR')) return '可以发布与管理自己的内容'
  return '可以浏览免费内容、收藏与订阅'
})

const abilities = computed(() => [
  { text: '浏览免费内容', enabled: true, hint: '所有访客都可以' },
  { text: '收藏内容', enabled: true, hint: '登录后即可' },
  { text: '订阅创作者', enabled: true, hint: '模拟支付，即时生效' },
  { text: '发布与编辑内容', enabled: isCreator.value, hint: '需要创作者身份' },
  { text: '审核内容 / 管理分类', enabled: userStore.hasRole('ADMIN'), hint: '需要管理员身份' },
])

async function apply() {
  applying.value = true
  applyMessage.value = ''
  try {
    const res = await applyCreator()
    if (res.data.success) {
      // 后端已更新角色，但当前 SecurityContext 还是旧角色，必须重新拉一次
      await userStore.fetchCurrentUser()
      applyMessage.value = '已获得创作者身份，现在可以发布内容了'
    } else {
      applyMessage.value = res.data.message || '申请失败'
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    applyMessage.value = err.response?.data?.message || '申请失败'
  } finally {
    applying.value = false
  }
}

async function handleLogout() {
  await userStore.logout()
  router.push('/')
}

onMounted(async () => {
  await userStore.fetchCurrentUser()
  try {
    const fav = await myFavorites(1, 6)
    if (fav.data.success) {
      favorites.value = fav.data.data.list
      favoriteTotal.value = fav.data.data.total
    }
  } catch { /* 忽略：收藏列表失败不影响主页 */ }
  try {
    const subs = await mySubscriptions(1, 50)
    if (subs.data.success) {
      activeSubCount.value = subs.data.data.list.filter((s) => s.valid).length
    }
  } catch { /* 忽略 */ }
  loading.value = false
})
</script>

<style scoped>
.profile-page { padding: 70px 0 30px; }
.profile-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-top: 34px; }
.profile-panel { border: 1px solid var(--line); padding: 26px; }
.profile-panel h3 { font-size: 20px; letter-spacing: -0.04em; margin: 0 0 20px; }
.info-list { margin: 0 0 22px; }
.info-list > div { display: flex; justify-content: space-between; gap: 16px; padding: 12px 0; border-bottom: 1px solid var(--line); }
.info-list dt { color: var(--muted); font: 11px 'DM Mono', monospace; }
.info-list dd { margin: 0; text-align: right; }
.panel-actions { display: flex; flex-wrap: wrap; gap: 10px; }
.ability-list { list-style: none; margin: 0; padding: 0; }
.ability-list li { display: flex; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--line); }
.ability-list li.disabled { color: var(--muted); }
.ability-list li span { width: 16px; }
.ability-list small { display: block; color: var(--muted); font: 10px 'DM Mono', monospace; margin-top: 5px; }
.sub-summary { margin-top: 20px; }
.sub-summary p { color: var(--muted); font-size: 13px; line-height: 1.8; }
.favorites { margin-top: 34px; border: 1px solid var(--line); padding: 26px; }
.favorites .panel-head { display: flex; justify-content: space-between; align-items: baseline; }
.favorites h3 { font-size: 20px; letter-spacing: -0.04em; margin: 0; }
.favorites .panel-head span { font: 10px 'DM Mono', monospace; color: var(--muted); }
.fav-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 18px; margin-top: 20px; }
.fav-card { border: 1px solid var(--line); padding: 18px; cursor: pointer; }
.fav-card:hover { transform: translateY(-3px); transition: transform 0.2s; }
.fav-type { font: 10px 'DM Mono', monospace; color: var(--muted); }
.fav-card h4 { margin: 10px 0 8px; font-size: 16px; letter-spacing: -0.03em; }
.fav-card p { margin: 0; color: var(--muted); font-size: 12px; line-height: 1.6; }
.fav-foot { display: flex; justify-content: space-between; margin-top: 14px; font: 10px 'DM Mono', monospace; color: var(--muted); }
.success-text { color: #68863d; font-size: 12px; margin-top: 12px; }
@media (max-width: 800px) {
  .profile-grid { grid-template-columns: 1fr; }
}
</style>
