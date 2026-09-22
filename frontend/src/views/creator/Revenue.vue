<template>
  <div class="revenue-page content-width">
    <RouterLink to="/creator" class="back-link">← 返回创作者工作台</RouterLink>

    <div class="section-heading">
      <div>
        <p class="eyebrow">CREATOR / REVENUE</p>
        <h2>收益<br /><em>订阅产生的收入</em></h2>
      </div>
      <p class="heading-aside">
        口径：已支付且<strong>未退款</strong>的订阅金额。<br />
        用户提前终止仍计入，只有退款才扣减。
      </p>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <div v-else-if="!revenue" class="empty-state">暂时拿不到收益数据。</div>
    <template v-else>
      <div class="metric-grid">
        <div class="metric-card">
          <span>累计收益</span>
          <strong>¥{{ money(revenue.totalRevenue) }}</strong>
          <small>已扣退款</small>
        </div>
        <div class="metric-card">
          <span>已退款</span>
          <strong>¥{{ money(revenue.refundedAmount) }}</strong>
          <small>单独列出，避免误读成毛收入</small>
        </div>
        <div class="metric-card">
          <span>订单总数</span>
          <strong>{{ revenue.subscriptionCount }}</strong>
          <small>含已终止与已退款</small>
        </div>
        <div class="metric-card">
          <span>生效中</span>
          <strong>{{ revenue.activeCount }}</strong>
          <small>当前仍可访问</small>
        </div>
      </div>

      <div class="two-col">
        <section class="panel">
          <h3>按套餐</h3>
          <div v-if="!revenue.byPlan.length" class="empty-state">还没有产生订单。</div>
          <table v-else class="rev-table">
            <thead>
              <tr><th>套餐</th><th>单价</th><th>单数</th><th>金额</th></tr>
            </thead>
            <tbody>
              <tr v-for="p in revenue.byPlan" :key="p.planId">
                <td class="cell-name">{{ p.planName }}</td>
                <td>¥{{ money(p.price) }}</td>
                <td>{{ p.count }}</td>
                <td class="cell-amount">¥{{ money(p.amount) }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section class="panel">
          <h3>按月</h3>
          <div v-if="!revenue.monthly.length" class="empty-state">还没有产生订单。</div>
          <ul v-else class="month-list">
            <li v-for="m in revenue.monthly" :key="m.month">
              <span class="month">{{ m.month }}</span>
              <div class="bar"><i :style="{ width: barWidth(m.amount) }"></i></div>
              <span class="month-amount">¥{{ money(m.amount) }} · {{ m.count }} 单</span>
            </li>
          </ul>
        </section>
      </div>

      <section class="panel">
        <h3>最近订单</h3>
        <div v-if="!revenue.recent.length" class="empty-state">还没有订单。</div>
        <table v-else class="rev-table">
          <thead>
            <tr><th>订单</th><th>套餐</th><th>用户</th><th>开始</th><th>到期</th><th>状态</th></tr>
          </thead>
          <tbody>
            <tr v-for="s in revenue.recent" :key="s.id">
              <td>#{{ s.id }}</td>
              <td class="cell-name">{{ s.planName || ('#' + s.planId) }}</td>
              <td>{{ s.creatorName ? '#' + s.creatorId : '#' + s.creatorId }}</td>
              <td class="cell-time">{{ s.startTime }}</td>
              <td class="cell-time">{{ s.endTime }}</td>
              <td>
                <span class="status-tag" :class="statusClass(s.status)">{{ statusLabel(s.status) }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getCreatorRevenue } from '@/api/creator'
import type { CreatorRevenue } from '@/api/types'

const loading = ref(true)
const revenue = ref<CreatorRevenue | null>(null)

/** 后端返回的是 BigDecimal，序列化后可能是字符串，统一按数字格式化 */
function money(v: number | string | null | undefined) {
  const n = typeof v === 'string' ? Number(v) : v
  return (n ?? 0).toFixed(2)
}

const maxMonthly = computed(() =>
  Math.max(1, ...(revenue.value?.monthly ?? []).map((m) => Number(m.amount)))
)
function barWidth(amount: number | string) {
  return `${Math.round((Number(amount) / maxMonthly.value) * 100)}%`
}

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: '生效中',
  EXPIRED: '已过期',
  CANCELED: '已终止',
  REFUNDED: '已退款',
}
const statusLabel = (s: string) => STATUS_LABEL[s] ?? s
const statusClass = (s: string) =>
  s === 'ACTIVE' ? 'is-active' : s === 'REFUNDED' ? 'is-refunded' : 'is-closed'

onMounted(async () => {
  try {
    const res = await getCreatorRevenue()
    if (res.data.success) revenue.value = res.data.data
  } catch {
    revenue.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.revenue-page {
  padding: 34px 0 40px;
}
.back-link {
  font: 11px 'DM Mono', monospace;
  color: var(--muted);
  text-decoration: none;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: var(--line);
  border: 1px solid var(--line);
  margin: 30px 0 34px;
}
.metric-card {
  background: var(--paper);
  padding: 24px;
}
.metric-card span {
  display: block;
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
}
.metric-card strong {
  display: block;
  font-size: 30px;
  letter-spacing: -0.06em;
  margin: 20px 0 10px;
}
.metric-card small {
  display: block;
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  line-height: 1.6;
}
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 34px;
}
.panel {
  border: 1px solid var(--line);
  padding: 24px;
}
.panel h3 {
  font-size: 18px;
  letter-spacing: -0.03em;
  margin: 0 0 18px;
}
.rev-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.rev-table th {
  text-align: left;
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  padding: 8px 6px;
  border-bottom: 1px solid var(--line);
}
.rev-table td {
  padding: 12px 6px;
  border-bottom: 1px solid var(--line);
}
.cell-name {
  font-weight: 600;
}
.cell-amount {
  font-weight: 600;
}
.cell-time {
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
}
.month-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.month-list li {
  display: grid;
  grid-template-columns: 66px 1fr 130px;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
}
.month {
  font: 11px 'DM Mono', monospace;
  color: var(--muted);
}
.bar {
  height: 6px;
  background: var(--line);
}
.bar i {
  display: block;
  height: 100%;
  background: var(--orange);
}
.month-amount {
  font: 11px 'DM Mono', monospace;
  text-align: right;
}
.status-tag {
  font: 10px 'DM Mono', monospace;
  padding: 2px 7px;
  border: 1px solid var(--line);
}
.status-tag.is-active {
  color: #68863d;
  border-color: #68863d;
}
.status-tag.is-refunded {
  color: #c54a32;
  border-color: #c54a32;
}
.status-tag.is-closed {
  color: var(--muted);
}
@media (max-width: 900px) {
  .metric-grid,
  .two-col {
    grid-template-columns: 1fr;
  }
}
</style>
