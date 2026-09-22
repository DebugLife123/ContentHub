<template>
  <div>
    <AdminPageHeader
      eyebrow="ADMIN / CREATOR APPLICATIONS"
      title="创作者申请"
      accent="审核通过后才有发布权限"
      :description="'待审核 ' + pendingTotal + ' 条。通过后用户角色升为 CREATOR，并自动建立创作者资料。'"
    >
      <template #actions>
        <el-button class="button button-dark" @click="load">刷新 <span>↗</span></el-button>
      </template>
    </AdminPageHeader>

    <div class="admin-toolbar">
      <el-select v-model="status" class="filter-select" @change="applyFilter">
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
      <span v-if="message" class="admin-flash" :class="messageType === 'error' ? 'is-error' : 'is-ok'">{{ message }}</span>
    </div>

    <p v-if="loading" class="admin-loading">LOADING…</p>
    <div v-else-if="!items.length" class="admin-empty">
      <span class="admin-empty-mark">···</span>
      <p>这个状态下没有申请。</p>
    </div>
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
          <td class="admin-cell-strong">
            {{ item.nickname || item.username || ('用户 #' + item.userId) }}
            <small>@{{ item.username }}</small>
          </td>
          <td>
            <div class="admin-cell-muted admin-cell-clip">{{ item.intro || '（未填写）' }}</div>
          </td>
          <td class="admin-cell-mono">{{ item.createTime || '—' }}</td>
          <td>
            <span class="admin-tag" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
            <div v-if="item.status === 'REJECTED' && item.rejectReason" class="reject-reason">
              {{ item.rejectReason }}
            </div>
          </td>
          <td class="admin-actions">
            <template v-if="item.status === 'PENDING'">
              <a :class="{ 'is-busy': actingId === item.id }" @click.prevent="approve(item)">通过</a>
              <a class="is-danger" :class="{ 'is-busy': actingId === item.id }"
                 @click.prevent="openReject(item)">驳回</a>
            </template>
            <span v-else class="admin-cell-mono">已处理</span>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="admin-pager">
      <el-pagination
        layout="prev, pager, next, total"
        :total="total"
        :current-page="pageNum"
        :page-size="pageSize"
        background
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog v-model="rejectVisible" class="admin-dialog" title="驳回申请" width="440px">
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
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

const loading = ref(true)
const saving = ref(false)
/** 正在审核的行 id：请求在途时禁用该行操作，避免连点产生两次审核 */
const actingId = ref<number | null>(null)
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

/** 状态 → admin-tag 变体（在 admin-system.scss 里统一定义） */
const STATUS_TAG_CLASS: Record<string, string> = {
  APPROVED: 'is-on',
  PENDING: 'is-warn',
  REJECTED: 'is-off',
}
const statusClass = (s: string) => STATUS_TAG_CLASS[s] ?? ''

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
  if (actingId.value !== null) return
  actingId.value = item.id
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
  } finally {
    actingId.value = null
  }
}

function openReject(item: CreatorApplication) {
  if (actingId.value !== null) return
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
/* 通用样式（页头 / 表格 / 标签 / 工具条 / 空态）统一在 styles/admin-system.scss，
   这里只保留本页特有的筛选宽度、昵称副行与驳回原因。 */
.filter-select {
  width: 150px;
}
.admin-cell-strong small {
  display: block;
  margin-top: 4px;
  font: 400 10px 'DM Mono', monospace;
  color: var(--muted);
}
.reject-reason {
  margin-top: 6px;
  font-size: 11px;
  color: #c54a32;
}
</style>
