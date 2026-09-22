<template>
  <div class="creator-page content-width">
    <RouterLink to="/contents" class="back-link">← 返回内容库</RouterLink>

    <div v-if="loading" class="empty-state">正在加载创作者主页…</div>
    <div v-else-if="error" class="empty-state">{{ error }}</div>
    <template v-else-if="profile">
      <!-- 头部 -->
      <header class="creator-hero">
        <span class="hero-avatar">{{ (profile.displayName || 'C').slice(0, 1) }}</span>
        <div class="hero-main">
          <h1>
            {{ profile.displayName }}
            <span v-if="profile.verified" class="verified" title="已认证">✓</span>
          </h1>
          <p class="hero-intro">{{ profile.intro || '这位创作者还没有填写简介。' }}</p>
          <div class="hero-meta">
            <span class="chip">{{ profile.publishedCount || 0 }} 篇已发布</span>
            <span class="chip">{{ profile.contentCount || 0 }} 篇内容</span>
            <span class="chip">{{ profile.subscriberCount || 0 }} 位订阅者</span>
          </div>
        </div>
        <div class="hero-actions">
          <el-button class="button button-dark" @click="$router.push('/plans')">
            查看订阅方案 <span>↗</span>
          </el-button>
        </div>
      </header>

      <!-- 订阅套餐 -->
      <section v-if="plans.length" class="creator-section">
        <h2>TA 的订阅方案</h2>
        <div class="plan-row">
          <article v-for="p in plans" :key="p.id" class="plan-card">
            <strong>{{ p.name }}</strong>
            <p>{{ p.description || '暂无描述' }}</p>
            <div class="plan-foot">
              <span class="price">¥{{ p.price }}</span>
              <span class="days">{{ p.durationDays }} 天</span>
            </div>
          </article>
        </div>
      </section>

      <!-- 发布的内容 -->
      <section class="creator-section">
        <h2>TA 发布的内容 <small>{{ total }}</small></h2>
        <div v-if="!items.length" class="empty-state">这位创作者还没有发布内容。</div>
        <div v-else class="content-grid">
          <article
            v-for="item in items"
            :key="item.id"
            class="content-card"
            @click="$router.push(`/content/${item.id}`)"
          >
            <div class="content-cover" :class="themeOf(item.id)">
              <span class="cover-type">{{ item.contentType }}</span>
              <strong>{{ coverText(item.title) }}</strong>
              <span class="cover-arrow">↗</span>
            </div>
            <div class="content-meta">
              <span>{{ item.categoryName || '未分类' }}</span>
              <span>{{ item.viewCount || 0 }} 次阅读</span>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.summary || '一份正在持续更新的数字内容。' }}</p>
            <div class="content-foot">
              <span>{{ item.accessType === 'FREE' ? '免费阅读' : '订阅后阅读' }}</span>
              <span>♡ {{ item.likeCount || 0 }}</span>
            </div>
          </article>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getCreatorProfile } from '@/api/creator'
import { pageContents } from '@/api/content'
import { listPlans } from '@/api/plan'
import type { ContentItem, CreatorProfile, SubscriptionPlan } from '@/api/types'

const route = useRoute()

const loading = ref(true)
const error = ref('')
const profile = ref<CreatorProfile | null>(null)
const items = ref<ContentItem[]>([])
const total = ref(0)
const plans = ref<SubscriptionPlan[]>([])

const themes = ['theme-orange', 'theme-lilac', 'theme-ink', 'theme-yellow']
const themeOf = (id: number) => themes[id % themes.length]
const coverText = (title: string) => (title || 'CONTENT').toUpperCase().split(' ').slice(0, 3).join('\n')

onMounted(async () => {
  const userId = Number(route.params.userId)
  if (Number.isNaN(userId)) {
    error.value = '创作者不存在'
    loading.value = false
    return
  }

  try {
    const res = await getCreatorProfile(userId)
    if (!res.data.success) throw new Error(res.data.message || '创作者不存在')
    profile.value = res.data.data
  } catch (e) {
    error.value = e instanceof Error ? e.message : '创作者主页加载失败'
    loading.value = false
    return
  }

  // 主页的三个区块互不依赖，任一失败都不该让整页报错
  try {
    const res = await pageContents({ pageNum: 1, pageSize: 9, creatorId: userId })
    if (res.data.success) {
      items.value = res.data.data.list
      total.value = res.data.data.total
    }
  } catch { /* 忽略 */ }

  try {
    const res = await listPlans()
    if (res.data.success) {
      plans.value = res.data.data.filter((p) => p.creatorId === userId)
    }
  } catch { /* 忽略 */ }

  loading.value = false
})
</script>

<style scoped>
.creator-page {
  padding: 34px 0 30px;
}
.back-link {
  font: 11px 'DM Mono', monospace;
  color: var(--muted);
  text-decoration: none;
}
.creator-hero {
  display: flex;
  align-items: flex-start;
  gap: 22px;
  border-bottom: 1px solid var(--line);
  padding-bottom: 30px;
  margin-top: 24px;
}
.hero-avatar {
  width: 74px;
  height: 74px;
  flex: 0 0 74px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--ink);
  color: #fff;
  font-size: 28px;
}
.hero-main {
  flex: 1;
  min-width: 0;
}
.hero-main h1 {
  font-size: clamp(28px, 3.2vw, 42px);
  line-height: 1.05;
  letter-spacing: -0.06em;
  margin: 0;
}
.verified {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--lime);
  color: var(--ink);
  font-size: 12px;
  vertical-align: middle;
  margin-left: 8px;
}
.hero-intro {
  max-width: 560px;
  margin: 14px 0 0;
  font-size: 14px;
  line-height: 1.85;
  color: var(--muted);
}
.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}
.chip {
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  border: 1px solid var(--line);
  padding: 4px 9px;
}
.hero-actions {
  flex: 0 0 auto;
}
.creator-section {
  margin-top: 40px;
}
.creator-section h2 {
  font-size: 20px;
  letter-spacing: -0.04em;
  margin: 0 0 20px;
}
.creator-section h2 small {
  font: 11px 'DM Mono', monospace;
  color: var(--muted);
  margin-left: 8px;
}
.plan-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 18px;
}
.plan-card {
  border: 1px solid var(--line);
  padding: 20px;
}
.plan-card strong {
  display: block;
  font-size: 16px;
  letter-spacing: -0.03em;
}
.plan-card p {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--muted);
}
.plan-foot {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-top: 16px;
  padding-top: 13px;
  border-top: 1px solid var(--line);
}
.plan-foot .price {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.04em;
}
.plan-foot .days {
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
}
</style>
