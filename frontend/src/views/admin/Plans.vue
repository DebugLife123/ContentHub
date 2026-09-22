<template>
  <div>
    <AdminPageHeader
      eyebrow="ADMIN / PLANS"
      title="套餐管理"
      accent="全平台订阅方案"
      description="仅管理员可见，展示所有创作者的套餐。套餐的增删改由创作者在各自工作台完成。"
    />

    <p v-if="loading" class="admin-loading">LOADING…</p>
    <div v-else-if="!items.length" class="admin-empty">
      <span class="admin-empty-mark">···</span>
      <p>还没有任何套餐。</p>
    </div>
    <table v-else class="admin-table plans-table">
      <thead>
        <tr><th>ID</th><th>创作者</th><th>名称</th><th>价格</th><th>周期</th><th>订阅人数</th><th>状态</th></tr>
      </thead>
      <tbody>
        <tr v-for="p in items" :key="p.id">
          <td class="admin-cell-mono">{{ p.id }}</td>
          <td class="admin-cell-muted">{{ p.creatorName || ('#' + p.creatorId) }}</td>
          <td class="admin-cell-strong">{{ p.name }}</td>
          <td class="admin-cell-mono">¥ {{ Number(p.price).toFixed(2) }}</td>
          <td class="admin-cell-mono">{{ p.durationDays }} 天</td>
          <td class="admin-cell-mono">{{ p.subscriberCount ?? 0 }}</td>
          <td>
            <span class="admin-tag" :class="statusClass(p.status)">
              {{ p.status === 'ACTIVE' ? '上架' : '下架' }}
            </span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listAllPlans } from '@/api/admin'
import type { SubscriptionPlan } from '@/api/types'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

const loading = ref(true)
const items = ref<SubscriptionPlan[]>([])

/** 状态 → admin-tag 变体（在 admin-system.scss 里统一定义） */
const STATUS_TAG_CLASS: Record<string, string> = {
  ACTIVE: 'is-on',
  INACTIVE: 'is-off',
}
function statusClass(s: string) {
  return STATUS_TAG_CLASS[s] ?? ''
}

onMounted(async () => {
  try {
    const res = await listAllPlans()
    if (res.data.success) items.value = res.data.data
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
/* 通用样式（页头 / 表格 / 标签 / 空态）统一在 styles/admin-system.scss。
   本页没有工具条，只补表格与页头之间的呼吸。 */
.plans-table { margin-top: 22px; }
</style>
