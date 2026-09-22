<template>
  <div class="admin-page content-width">
    <div class="section-heading">
      <div>
        <p class="eyebrow">ADMIN / CREATOR APPLICATIONS</p>
        <h2>创作者申请<br /><em>审核通过后才有发布权限</em></h2>
      </div>
      <p class="heading-aside">
        待审核 <strong>{{ pendingTotal }}</strong> 条<br />
        通过后用户角色升为 CREATOR，并自动建立创作者资料。
      </p>
    </div>

    <div class="toolbar">
      <el-select v-model="status" class="filter-select" @change="applyFilter">
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
      <el-button class="button button-dark" @click="load">刷新</el-button>
      <span v-if="message" :class="messageType === 'error' ? 'error-text' : 'success-text'">{{ message }}</span>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <div v-else-if="!items.length" class="empty-state">这个状态下没有申请。</div>
    <table v-else class="admin-table">
      <thead>
        <tr>
          <th>申请人</th>
          <th>申请说明</th>
          <th>提交时间</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td class="cell-name">
            {{ item.nickname || item.username || ('用户 #' + item.userId) }}
            <small>@{{ item.username }}</small>
          </td>
          <td class="cell-intro">{{ item.intro || '（未填写）' }}</td>
          <td class="cell-time">{{ item.createTime || '—' }}</td>
          <td>
            <span class="status-tag" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
            <div v-if="item.status === 'REJECTED' && item.rejectReason" class="reject-reason">
              {{ item.rejectReason }}
            </div>
          </td>
          <td class="cell-actions">
            <template v-if="item.status === 'PENDING'">
              <a @click.prevent="approve(item)">通过</a>
              <a class="danger" @click.prevent="openReject(item)">驳回</a>
            </template>
            <span v-else class="muted">已处理</span>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="pager">
      <el-pagination
        layout="prev, pager, next, total"
        :total="total"
        :current-page="pageNum"
        :page-size="pageSize"
        background
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog v-model="rejectVisible" title="驳回申请" width="440px">
      <el-input v-model="rejectReason" type="textarea" :rows="4" maxlength="500" show-word-limit
                placeholder="写清楚原因，申请人会在站内通知里看到" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button class="button button-dark" :loading="saving" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  approveCreatorApplication,
  creatorApplicationPendingCount,
  pageCreatorApplications,
  rejectCreatorApplication,
} from '@/api/admin'
import type { CreatorApplication } from '@/api/types'

const loading = ref(true)
const saving = ref(false)
const items = ref<CreatorApplication[]>([])
const total = ref(0)
const pendingTotal = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('PENDING')

const message = ref('')
const messageType = ref<'success' | 'error'>('success')
const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectTarget = ref<CreatorApplication | null>(null)

const STATUS_LABEL: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
}
const statusLabel = (s: string) => STATUS_LABEL[s] ?? s
const statusClass = (s: string) =>
  s === 'APPROVED' ? 'status-approved' : s === 'REJECTED' ? 'status-rejected' : 'status-pending'

function flash(text: string, type: 'success' | 'error' = 'success') {
  message.value = text
  messageType.value = type
  setTimeout(() => (message.value = ''), 3000)
}

function errText(e: unknown, fallback: string) {
  const err = e as { message?: string; response?: { data?: { message?: string } } }
  return err.response?.data?.message || err.message || fallback
}

async function load() {
  loading.value = true
  try {
    const res = await pageCreatorApplications({
      status: status.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    if (res.data.success) {
      items.value = res.data.data.list
      total.value = res.data.data.total
    }
  } catch (e) {
    items.value = []
    total.value = 0
    flash(errText(e, '加载失败'), 'error')
  } finally {
    loading.value = false
  }
}

async function loadPendingCount() {
  try {
    const res = await creatorApplicationPendingCount()
    if (res.data.success) pendingTotal.value = res.data.data
  } catch {
    pendingTotal.value = 0
  }
}

function applyFilter() {
  pageNum.value = 1
  load()
}

function handlePageChange(p: number) {
  pageNum.value = p
  load()
}

async function approve(item: CreatorApplication) {
  try {
    const res = await approveCreatorApplication(item.id)
    if (res.data.success) {
      flash('已通过，用户现在可以发布内容了')
      await Promise.all([load(), loadPendingCount()])
    } else {
      flash(res.data.message || '操作失败', 'error')
    }
  } catch (e) {
    flash(errText(e, '操作失败'), 'error')
  }
}

function openReject(item: CreatorApplication) {
  rejectTarget.value = item
  rejectReason.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  if (!rejectTarget.value) return
  if (!rejectReason.value.trim()) {
    flash('请填写驳回原因', 'error')
    return
  }
  saving.value = true
  try {
    const res = await rejectCreatorApplication(rejectTarget.value.id, rejectReason.value.trim())
    if (res.data.success) {
      rejectVisible.value = false
      flash('已驳回')
      await Promise.all([load(), loadPendingCount()])
    } else {
      flash(res.data.message || '操作失败', 'error')
    }
  } catch (e) {
    flash(errText(e, '操作失败'), 'error')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await Promise.all([load(), loadPendingCount()])
})
</script>

<style scoped>
.admin-page {
  padding: 60px 0 30px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 20px 0 8px;
}
.filter-select {
  width: 150px;
}
.admin-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 12px;
}
.admin-table th {
  text-align: left;
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  padding: 10px 8px;
  border-bottom: 1px solid var(--line);
}
.admin-table td {
  padding: 13px 8px;
  border-bottom: 1px solid var(--line);
  vertical-align: top;
}
.cell-name {
  font-weight: 600;
  white-space: nowrap;
}
.cell-name small {
  display: block;
  margin-top: 4px;
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  font-weight: 400;
}
.cell-intro {
  max-width: 380px;
  color: var(--muted);
  line-height: 1.6;
}
.cell-time {
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  white-space: nowrap;
}
.reject-reason {
  margin-top: 6px;
  font-size: 11px;
  color: #c54a32;
}
.cell-actions a {
  margin-right: 12px;
  cursor: pointer;
  text-decoration: underline;
  white-space: nowrap;
}
.cell-actions a.danger {
  color: #c54a32;
}
.muted {
  color: var(--muted);
  font: 10px 'DM Mono', monospace;
}
.status-tag {
  font: 10px 'DM Mono', monospace;
  padding: 2px 7px;
  border: 1px solid var(--line);
  white-space: nowrap;
}
.status-approved {
  color: #68863d;
  border-color: #68863d;
}
.status-pending {
  color: #a8481f;
  border-color: #e8b394;
}
.status-rejected {
  color: #c54a32;
  border-color: #c54a32;
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding: 30px 0 10px;
}
.success-text {
  color: #68863d;
  font-size: 12px;
}
</style>
