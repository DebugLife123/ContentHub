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
    <div v-else-if="loadError" class="empty-state" role="alert"><p>{{ loadError }}</p><el-button @click="load">重试</el-button></div>
    <div v-else-if="!items.length" class="empty-state">
      还没有订阅记录，去 <RouterLink to="/plans">订阅方案</RouterLink> 看看。
    </div>
    <table v-else class="sub-table">
      <thead>
        <tr><th>套餐</th><th>创作者</th><th>开始时间</th><th>到期时间</th><th>剩余</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td class="cell-name">{{ item.planName || '—' }}</td>
          <td>{{ item.creatorName || ('#' + item.creatorId) }}</td>
          <td class="cell-time">{{ item.startTime }}</td>
          <td class="cell-time">{{ item.endTime }}</td>
          <td>{{ item.valid ? item.remainingDays + ' 天' : '—' }}</td>
          <td>
            <span class="status-tag" :class="statusClass(item)">
              {{ statusLabel(item) }}
            </span>
          </td>
          <td class="cell-actions">
            <template v-if="item.valid">
              <a :class="{ disabled: actingId === item.id }" @click.prevent="handleCancel(item)">{{ actingId === item.id ? '处理中…' : '提前终止' }}</a>
              <a class="danger" :class="{ disabled: actingId === item.id }" @click.prevent="handleRefund(item)">申请退款</a>
            </template>
            <span v-else class="muted">—</span>
          </td>
        </tr>
      </tbody>
    </table>

    <p class="foot-note">
      提前终止与退款都会<strong>立即失去</strong>对应内容的访问权限——后端鉴权看的是
      「状态为生效中且未到期」，状态一变就失效，不需要等定时任务。
    </p>

    <div class="pager">
      <el-pagination layout="prev, pager, next, total" :total="total" :current-page="pageNum"
                     :page-size="pageSize" background @current-change="handlePageChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelSubscription, mySubscriptions, refundSubscription } from '@/api/subscription'
import type { Subscription } from '@/api/types'

const loading = ref(true)
const loadError = ref('')
const actingId = ref<number | null>(null)
const items = ref<Subscription[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: '生效中',
  EXPIRED: '已过期',
  CANCELED: '已终止',
  REFUNDED: '已退款',
}
/** 库里 status 可能仍是 ACTIVE 但时间已过，后端会把它纠正成 EXPIRED 再下发 */
function statusLabel(s: Subscription) {
  return STATUS_LABEL[s.status] ?? s.status
}
function statusClass(s: Subscription) {
  if (s.status === 'ACTIVE') return 'status-on'
  if (s.status === 'REFUNDED') return 'status-refunded'
  return 'status-off'
}

function errText(e: unknown, fallback: string) {
  const err = e as { message?: string; response?: { data?: { message?: string } } }
  return err.response?.data?.message || err.message || fallback
}

async function handleCancel(item: Subscription) {
  if (actingId.value !== null) return
  actingId.value = item.id
  try {
    await ElMessageBox.confirm(
      '终止后你会立即失去该创作者付费内容的访问权限，且无法恢复。确定吗？',
      '提前终止订阅',
      { confirmButtonText: '确定终止', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    actingId.value = null
    return
  }
  try {
    const res = await cancelSubscription(item.id)
    if (res.data.success) {
      ElMessage.success('已终止，访问权限已收回')
      await load()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error(errText(e, '操作失败'))
  } finally {
    actingId.value = null
  }
}

async function handleRefund(item: Subscription) {
  if (actingId.value !== null) return
  actingId.value = item.id
  try {
    await ElMessageBox.confirm(
      '这是模拟退款：订阅会置为「已退款」并从创作者收益中扣除，你会立即失去访问权限。确定吗？',
      '申请退款',
      { confirmButtonText: '确定退款', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    actingId.value = null
    return
  }
  try {
    const res = await refundSubscription(item.id)
    if (res.data.success) {
      ElMessage.success('已退款')
      await load()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error(errText(e, '操作失败'))
  } finally {
    actingId.value = null
  }
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await mySubscriptions(pageNum.value, pageSize.value)
    if (!res.data.success) throw new Error(res.data.message || '订阅记录加载失败')
    items.value = res.data.data.list
    total.value = res.data.data.total
  } catch (e) {
    loadError.value = errText(e, '订阅记录加载失败，请重试')
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
.status-refunded { color: #c54a32; border-color: #c54a32; }
.cell-actions a {
  margin-right: 12px;
  cursor: pointer;
  text-decoration: underline;
  white-space: nowrap;
}
.cell-actions a.danger { color: #c54a32; }
.cell-actions a.disabled { pointer-events: none; opacity: 0.45; }
.muted { color: var(--muted); }
.foot-note {
  margin-top: 22px;
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--muted);
  max-width: 640px;
}
.pager { display: flex; justify-content: flex-end; padding-top: 22px; }
</style>
