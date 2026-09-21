<template>
  <div class="admin-page content-width">
    <div class="section-heading">
      <div><p class="eyebrow">ADMIN / PLANS</p><h2>套餐管理<br><em>全平台订阅方案</em></h2></div>
      <p class="heading-aside">
        仅管理员可见，展示所有创作者的套餐。<br>
        套餐的增删改由创作者在各自工作台完成。
      </p>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <div v-else-if="!items.length" class="empty-state">还没有任何套餐。</div>
    <table v-else class="admin-table">
      <thead>
        <tr><th>ID</th><th>创作者</th><th>名称</th><th>价格</th><th>周期</th><th>订阅人数</th><th>状态</th></tr>
      </thead>
      <tbody>
        <tr v-for="p in items" :key="p.id">
          <td>{{ p.id }}</td>
          <td>{{ p.creatorName || ('#' + p.creatorId) }}</td>
          <td class="cell-name">{{ p.name }}</td>
          <td>¥ {{ Number(p.price).toFixed(2) }}</td>
          <td>{{ p.durationDays }} 天</td>
          <td>{{ p.subscriberCount ?? 0 }}</td>
          <td>
            <span class="status-tag" :class="p.status === 'ACTIVE' ? 'status-on' : 'status-off'">
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

const loading = ref(true)
const items = ref<SubscriptionPlan[]>([])

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
.admin-page { padding: 60px 0 30px; }
.admin-table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 30px; }
.admin-table th {
  text-align: left; font: 10px 'DM Mono', monospace; color: var(--muted);
  padding: 10px 8px; border-bottom: 1px solid var(--line);
}
.admin-table td { padding: 13px 8px; border-bottom: 1px solid var(--line); }
.cell-name { font-weight: 600; }
.status-tag { font: 10px 'DM Mono', monospace; padding: 2px 7px; border: 1px solid var(--line); }
.status-on { color: #68863d; border-color: #68863d; }
.status-off { color: var(--muted); }
</style>
