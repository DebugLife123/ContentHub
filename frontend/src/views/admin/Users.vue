<template>
  <div class="admin-page content-width">
    <div class="section-heading">
      <div><p class="eyebrow">ADMIN / USERS</p><h2>用户管理<br><em>启用与禁用</em></h2></div>
      <p class="heading-aside">仅管理员可见。<br>禁用后该账号无法登录（已登录的 token 在过期前仍有效）。</p>
    </div>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名或昵称" clearable style="width: 220px"
                @keyup.enter="applyFilter" @clear="applyFilter" />
      <el-select v-model="role" placeholder="全部角色" clearable style="width: 150px" @change="applyFilter">
        <el-option label="普通用户" value="USER" />
        <el-option label="创作者" value="CREATOR" />
        <el-option label="管理员" value="ADMIN" />
      </el-select>
      <el-button class="button button-dark" @click="applyFilter">筛选</el-button>
      <span v-if="message" :class="messageType === 'error' ? 'error-text' : 'success-text'">{{ message }}</span>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <table v-else class="admin-table">
      <thead>
        <tr><th>ID</th><th>用户名</th><th>昵称</th><th>角色</th><th>内容数</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="u in items" :key="u.id">
          <td>{{ u.id }}</td>
          <td class="cell-name">{{ u.username }}</td>
          <td>{{ u.nickname || '—' }}</td>
          <td>
            <span class="role-tag" :class="`role-${u.role.toLowerCase()}`">{{ roleLabel(u.role) }}</span>
          </td>
          <td>{{ u.contentCount ?? 0 }}</td>
          <td>
            <span class="status-tag" :class="u.status === 'ENABLED' ? 'status-on' : 'status-off'">
              {{ u.status === 'ENABLED' ? '正常' : '已禁用' }}
            </span>
          </td>
          <td class="cell-actions">
            <a v-if="u.status === 'ENABLED'" class="danger" @click.prevent="setStatus(u, 'DISABLED')">禁用</a>
            <a v-else @click.prevent="setStatus(u, 'ENABLED')">启用</a>
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
import { ElMessageBox } from 'element-plus'
import { pageAdminUsers, updateUserStatus } from '@/api/admin'
import type { AdminUser } from '@/api/types'

const loading = ref(true)
const items = ref<AdminUser[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const role = ref('')
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

const ROLE_LABELS: Record<string, string> = { USER: '普通用户', CREATOR: '创作者', ADMIN: '管理员' }
function roleLabel(r: string) {
  return ROLE_LABELS[r] ?? r
}

function flash(text: string, type: 'success' | 'error' = 'success') {
  message.value = text
  messageType.value = type
  setTimeout(() => (message.value = ''), 3000)
}

async function load() {
  loading.value = true
  try {
    const res = await pageAdminUsers({
      keyword: keyword.value || undefined,
      role: role.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
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

function applyFilter() {
  pageNum.value = 1
  load()
}
function handlePageChange(p: number) {
  pageNum.value = p
  load()
}

async function setStatus(u: AdminUser, status: 'ENABLED' | 'DISABLED') {
  if (status === 'DISABLED') {
    try {
      await ElMessageBox.confirm(
        `确定禁用「${u.username}」吗？禁用后该账号将无法登录。`, '禁用确认',
        { confirmButtonText: '禁用', cancelButtonText: '取消', type: 'warning' })
    } catch {
      return
    }
  }
  try {
    const res = await updateUserStatus(u.id, status)
    if (res.data.success) {
      flash(status === 'DISABLED' ? '已禁用' : '已启用')
      await load()
    } else {
      flash(res.data.message || '操作失败', 'error')
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    flash(err.response?.data?.message || '操作失败', 'error')
  }
}

onMounted(load)
</script>

<style scoped>
.admin-page { padding: 60px 0 30px; }
.toolbar { display: flex; align-items: center; gap: 12px; padding: 20px 0 8px; flex-wrap: wrap; }
.admin-table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 12px; }
.admin-table th {
  text-align: left; font: 10px 'DM Mono', monospace; color: var(--muted);
  padding: 10px 8px; border-bottom: 1px solid var(--line);
}
.admin-table td { padding: 13px 8px; border-bottom: 1px solid var(--line); }
.cell-name { font-weight: 600; }
.cell-actions a { margin-right: 12px; cursor: pointer; text-decoration: underline; }
.cell-actions a.danger { color: #c54a32; }
.status-tag, .role-tag { font: 10px 'DM Mono', monospace; padding: 2px 7px; border: 1px solid var(--line); }
.status-on { color: #68863d; border-color: #68863d; }
.status-off { color: #c54a32; border-color: #c54a32; }
.role-admin { color: #c07a1f; border-color: #c07a1f; }
.role-creator { color: #68863d; border-color: #68863d; }
.role-user { color: var(--muted); }
.pager { display: flex; justify-content: flex-end; padding-top: 22px; }
.success-text { color: #68863d; font-size: 12px; }
</style>
