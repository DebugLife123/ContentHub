<template>
  <div>
    <AdminPageHeader
      eyebrow="ADMIN / USERS"
      title="用户管理"
      accent="启用与禁用"
      description="仅管理员可见。禁用后该账号无法登录（已登录的 token 在过期前仍有效）。"
    />

    <div class="admin-toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名或昵称" clearable style="width: 220px"
                @keyup.enter="applyFilter" @clear="applyFilter" />
      <el-select v-model="role" placeholder="全部角色" clearable style="width: 150px" @change="applyFilter">
        <el-option label="普通用户" value="USER" />
        <el-option label="创作者" value="CREATOR" />
        <el-option label="管理员" value="ADMIN" />
      </el-select>
      <el-button class="button button-dark" @click="applyFilter">筛选</el-button>
      <span v-if="message" class="admin-flash" :class="messageType === 'error' ? 'is-error' : 'is-ok'">{{ message }}</span>
    </div>

    <p v-if="loading" class="admin-loading">LOADING…</p>
    <table v-else class="admin-table">
      <thead>
        <tr><th>ID</th><th>用户名</th><th>昵称</th><th>角色</th><th>内容数</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="u in items" :key="u.id">
          <td class="admin-cell-mono">{{ u.id }}</td>
          <td class="admin-cell-strong">{{ u.username }}</td>
          <td class="admin-cell-muted">{{ u.nickname || '—' }}</td>
          <td>
            <span class="admin-tag" :class="roleClass(u.role)">{{ roleLabel(u.role) }}</span>
          </td>
          <td class="admin-cell-mono">{{ u.contentCount ?? 0 }}</td>
          <td>
            <span class="admin-tag" :class="statusClass(u.status)">{{ u.status === 'ENABLED' ? '正常' : '已禁用' }}</span>
          </td>
          <td class="admin-actions">
            <a v-if="u.status === 'ENABLED'" class="is-danger" @click.prevent="setStatus(u, 'DISABLED')">禁用</a>
            <a v-else @click.prevent="setStatus(u, 'ENABLED')">启用</a>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="admin-pager">
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
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

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

/** 角色 → admin-tag 变体（在 admin-system.scss 里统一定义） */
const ROLE_TAG_CLASS: Record<string, string> = {
  ADMIN: 'is-warn',
  CREATOR: 'is-on',
  USER: '',
}
function roleClass(r: string) {
  return ROLE_TAG_CLASS[r] ?? ''
}

/** 状态 → admin-tag 变体 */
const STATUS_TAG_CLASS: Record<string, string> = {
  ENABLED: 'is-on',
  DISABLED: 'is-off',
}
function statusClass(s: string) {
  return STATUS_TAG_CLASS[s] ?? ''
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
