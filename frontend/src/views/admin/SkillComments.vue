<template>
  <div>
    <AdminPageHeader
      eyebrow="ADMIN / SKILL COMMENTS"
      title="Skill 评论管理"
      accent="隐藏或删除不当内容"
      description="隐藏后前台看不到，数据仍在。内容库的评论在「评论管理」里，两边是独立的表。"
    />

    <div class="admin-toolbar">
      <el-select v-model="status" class="filter-select" @change="reload">
        <el-option label="全部" value="" />
        <el-option label="正常" value="NORMAL" />
        <el-option label="已隐藏" value="HIDDEN" />
      </el-select>
      <span v-if="message" class="admin-flash" :class="messageType === 'error' ? 'is-error' : 'is-ok'">{{ message }}</span>
    </div>

    <p v-if="loading" class="admin-loading">LOADING…</p>
    <div v-else-if="!items.length" class="admin-empty">
      <span class="admin-empty-mark">···</span>
      <p>没有符合条件的评论。</p>
    </div>
    <table v-else class="admin-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Skill</th>
          <th>用户</th>
          <th>内容</th>
          <th>状态</th>
          <th>时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="c in items" :key="c.id">
          <td class="admin-cell-mono">{{ c.id }}</td>
          <td class="admin-cell-strong">{{ c.skillName || ('#' + c.skillId) }}</td>
          <td class="admin-cell-muted">{{ c.username || ('#' + c.userId) }}</td>
          <td>
            <div class="admin-cell-clip">{{ c.body }}</div>
          </td>
          <td>
            <span class="admin-tag" :class="statusClass(c.status)">
              {{ c.status === 'NORMAL' ? '正常' : '已隐藏' }}
            </span>
          </td>
          <td class="admin-cell-mono">{{ c.createTime || '—' }}</td>
          <td class="admin-actions">
            <a v-if="c.status === 'NORMAL'" @click.prevent="setStatus(c, 'HIDDEN')">隐藏</a>
            <a v-else @click.prevent="setStatus(c, 'NORMAL')">恢复</a>
            <a class="is-danger" @click.prevent="remove(c)">删除</a>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import {
  deleteSkillCommentAsAdmin,
  pageAdminSkillComments,
  updateSkillCommentStatus,
  type SkillComment,
} from '@/api/skillComment'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

const loading = ref(true)
const items = ref<SkillComment[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('')
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

/** 状态 → admin-tag 变体（在 admin-system.scss 里统一定义） */
const STATUS_TAG_CLASS: Record<string, string> = {
  NORMAL: 'is-on',
  HIDDEN: 'is-off',
}
function statusClass(s: string) {
  return STATUS_TAG_CLASS[s] ?? ''
}

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
    const res = await pageAdminSkillComments({
      status: status.value || undefined,
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

function reload() {
  pageNum.value = 1
  load()
}

function handlePageChange(p: number) {
  pageNum.value = p
  load()
}

async function setStatus(c: SkillComment, next: 'NORMAL' | 'HIDDEN') {
  try {
    const res = await updateSkillCommentStatus(c.id, next)
    if (res.data.success) {
      flash(next === 'HIDDEN' ? '已隐藏' : '已恢复')
      await load()
    } else {
      flash(res.data.message || '操作失败', 'error')
    }
  } catch (e) {
    flash(errText(e, '操作失败'), 'error')
  }
}

async function remove(c: SkillComment) {
  try {
    await ElMessageBox.confirm('删除后不可恢复，确定吗？', '删除评论', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    const res = await deleteSkillCommentAsAdmin(c.id)
    if (res.data.success) {
      flash('已删除')
      await load()
    } else {
      flash(res.data.message || '删除失败', 'error')
    }
  } catch (e) {
    flash(errText(e, '删除失败'), 'error')
  }
}

onMounted(load)
</script>

<style scoped>
/* 通用样式（页头 / 表格 / 标签 / 工具条 / 空态）统一在 styles/admin-system.scss，
   这里只保留本页特有的筛选框宽度。 */
.filter-select {
  width: 150px;
}
</style>
