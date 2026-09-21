<template>
  <div class="my-sub-page content-width">
    <div class="section-heading">
      <div><p class="eyebrow">MY SUBSCRIPTIONS</p><h2>我的订阅<br><em>生效中的与已过期的</em></h2></div>
      <p class="heading-aside">
        共 {{ total }} 条订阅记录。<br>
        过期后对应内容会自动重新锁定。
      </p>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <div v-else-if="!items.length" class="empty-state">
      还没有订阅记录，去 <RouterLink to="/plans">订阅方案</RouterLink> 看看。
    </div>
    <table v-else class="sub-table">
      <thead>
        <tr><th>套餐</th><th>创作者</th><th>开始时间</th><th>到期时间</th><th>剩余</th><th>状态</th></tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td class="cell-name">{{ item.planName || '—' }}</td>
          <td>{{ item.creatorName || ('#' + item.creatorId) }}</td>
          <td class="cell-time">{{ item.startTime }}</td>
          <td class="cell-time">{{ item.endTime }}</td>
          <td>{{ item.valid ? item.remainingDays + ' 天' : '—' }}</td>
          <td>
            <span class="status-tag" :class="item.valid ? 'status-on' : 'status-off'">
              {{ item.valid ? '生效中' : '已过期' }}
            </span>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="pager">
      <el-pagination layout="prev, pager, next, total" :total="total" :current-page="pageNum"
                     :page-size="pageSize" background @current-change="handlePageChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { mySubscriptions } from '@/api/subscription'
import type { Subscription } from '@/api/types'

const loading = ref(true)
const items = ref<Subscription[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

async function load() {
  loading.value = true
  try {
    const res = await mySubscriptions(pageNum.value, pageSize.value)
    if (res.data.success) {
      items.value = res.data.data.list
      total.value = res.data.data.total
    }
  } catch {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  pageNum.value = page
  load()
}

onMounted(load)
</script>

<style scoped>
.my-sub-page { padding: 60px 0 30px; }
.sub-table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 30px; }
.sub-table th {
  text-align: left; font: 10px 'DM Mono', monospace; color: var(--muted);
  padding: 10px 8px; border-bottom: 1px solid var(--line);
}
.sub-table td { padding: 13px 8px; border-bottom: 1px solid var(--line); }
.cell-name { font-weight: 600; }
.cell-time { font: 10px 'DM Mono', monospace; color: var(--muted); }
.status-tag { font: 10px 'DM Mono', monospace; padding: 2px 7px; border: 1px solid var(--line); }
.status-on { color: #68863d; border-color: #68863d; }
.status-off { color: var(--muted); }
.pager { display: flex; justify-content: flex-end; padding-top: 22px; }
</style>
