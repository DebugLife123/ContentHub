<template>
  <div>
    <AdminPageHeader
      eyebrow="ADMIN / OVERVIEW"
      title="控制台概览"
      accent="全局态势"
      description="平台待处理事项与整体规模。数字来自各管理模块的实时统计，点击可直接跳转处理。"
    />

    <!-- 指标 -->
    <div class="admin-metrics">
      <RouterLink
        v-for="metric in metrics"
        :key="metric.label"
        class="admin-metric"
        :class="{ 'is-accent': metric.accent && (metric.value ?? 0) > 0 }"
        :to="metric.to"
      >
        <span class="admin-metric-label">{{ metric.label }}</span>
        <strong class="admin-metric-value">{{ fmt(metric.value) }}</strong>
        <span class="admin-metric-foot">{{ metric.foot }}</span>
      </RouterLink>
    </div>

    <!-- 待审核队列 -->
    <p class="admin-section-title">待审核队列</p>

    <div class="admin-panel">
      <div class="admin-panel-head">
        <h2 class="admin-panel-title">最新待审核内容</h2>
        <div class="admin-actions">
          <RouterLink to="/admin/contents">全部内容审核 ↗</RouterLink>
        </div>
      </div>

      <div class="admin-panel-body">
        <p v-if="loading" class="admin-loading">LOADING…</p>
        <div v-else-if="!recentPending.length" class="admin-empty">
          <span class="admin-empty-mark">···</span>
          <p>审核队列是空的，暂时没有待处理投稿。</p>
        </div>
        <table v-else class="admin-table">
          <thead>
            <tr><th>ID</th><th>标题</th><th>分类</th><th>作者</th><th>访问</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in recentPending" :key="item.id">
              <td class="admin-cell-mono">{{ item.id }}</td>
              <td class="admin-cell-strong">{{ item.title }}</td>
              <td class="admin-cell-muted">{{ item.categoryName || '—' }}</td>
              <td class="admin-cell-muted">#{{ item.creatorId }}</td>
              <td>
                <span class="admin-tag" :class="item.accessType === 'FREE' ? 'is-info' : 'is-warn'">
                  {{ item.accessType === 'FREE' ? '免费' : '订阅' }}
                </span>
              </td>
              <td class="admin-actions">
                <RouterLink to="/admin/contents">去审核</RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 快捷入口 -->
    <p class="admin-section-title">模块直达</p>
    <div class="admin-quick">
      <RouterLink
        v-for="entry in quickEntries"
        :key="entry.to"
        class="admin-quick-item"
        :to="entry.to"
      >
        <span class="admin-quick-no">{{ entry.no }}</span>
        <span>
          <strong>{{ entry.label }}</strong>
          <small>{{ entry.desc }}</small>
        </span>
        <span class="admin-quick-arrow">→</span>
      </RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { pageForReview } from '@/api/content'
import { listAllPlans, pageAdminComments, pageAdminUsers } from '@/api/admin'
import { listAllCategories } from '@/api/category'
import type { ContentItem } from '@/api/types'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

/**
 * 控制台概览。
 *
 * 五个统计口径各自独立请求、各自容错：任一接口失败只让对应数字显示「—」，
 * 不影响整页可用（后端未启动时也能看到壳和入口）。
 */
const loading = ref(true)
const pendingContents = ref<number | null>(null)
const totalUsers = ref<number | null>(null)
const totalComments = ref<number | null>(null)
const totalCategories = ref<number | null>(null)
const planStats = ref<{ total: number; active: number } | null>(null)
const recentPending = ref<ContentItem[]>([])

const metrics = computed(() => [
  {
    label: '待审核内容',
    value: pendingContents.value,
    foot: '需要处理的投稿',
    to: '/admin/contents',
    accent: true,
  },
  {
    label: '注册用户',
    value: totalUsers.value,
    foot: '含用户 / 创作者 / 管理员',
    to: '/admin/users',
    accent: false,
  },
  {
    label: '评论总数',
    value: totalComments.value,
    foot: '可隐藏或删除',
    to: '/admin/comments',
    accent: false,
  },
  {
    label: '内容分类',
    value: totalCategories.value,
    foot: '内容库的分类维度',
    to: '/admin/categories',
    accent: false,
  },
  {
    label: '订阅套餐',
    value: planStats.value?.total ?? null,
    foot: planStats.value ? `其中 ${planStats.value.active} 个上架中` : '全平台创作者套餐',
    to: '/admin/plans',
    accent: false,
  },
])

const quickEntries = [
  { to: '/admin/contents', no: '01', label: '内容审核', desc: '通过或驳回投稿，查看排版预览' },
  { to: '/admin/comments', no: '02', label: '评论管理', desc: '隐藏违规评论或彻底删除' },
  { to: '/admin/users', no: '03', label: '用户管理', desc: '启停账号、查看角色与内容数' },
  { to: '/admin/categories', no: '04', label: '分类管理', desc: '维护内容库的分类与排序' },
  { to: '/admin/plans', no: '05', label: '套餐管理', desc: '查看全平台订阅方案与订阅人数' },
]

function fmt(value: number | null): string {
  return value === null ? '—' : String(value)
}

onMounted(async () => {
  await Promise.allSettled([
    (async () => {
      const res = await pageForReview({ status: 'PENDING', pageNum: 1, pageSize: 5 })
      if (res.data.success) {
        pendingContents.value = res.data.data.total
        recentPending.value = res.data.data.list
      }
    })(),
    (async () => {
      const res = await pageAdminUsers({ pageNum: 1, pageSize: 1 })
      if (res.data.success) totalUsers.value = res.data.data.total
    })(),
    (async () => {
      const res = await pageAdminComments({ pageNum: 1, pageSize: 1 })
      if (res.data.success) totalComments.value = res.data.data.total
    })(),
    (async () => {
      const res = await listAllCategories()
      if (res.data.success) totalCategories.value = res.data.data.length
    })(),
    (async () => {
      const res = await listAllPlans()
      if (res.data.success) {
        planStats.value = {
          total: res.data.data.length,
          active: res.data.data.filter((plan) => plan.status === 'ACTIVE').length,
        }
      }
    })(),
  ])
  loading.value = false
})
</script>
