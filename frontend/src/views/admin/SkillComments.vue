<template>
  <div class="admin-page content-width">
    <div class="section-heading">
      <div>
        <p class="eyebrow">ADMIN / SKILL COMMENTS</p>
        <h2>Skill 评论管理<br /><em>隐藏或删除不当内容</em></h2>
      </div>
      <p class="heading-aside">
        隐藏后前台看不到，数据仍在。<br />
        内容库的评论在「评论管理」里，两边是独立的表。
      </p>
    </div>

    <div class="toolbar">
      <el-select v-model="status" class="filter-select" @change="reload">
        <el-option label="全部" value="" />
        <el-option label="正常" value="NORMAL" />
        <el-option label="已隐藏" value="HIDDEN" />
      </el-select>
      <span v-if="message" :class="messageType === 'error' ? 'error-text' : 'success-text'">{{ message }}</span>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <div v-else-if="!items.length" class="empty-state">没有符合条件的评论。</div>
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
          <td>{{ c.id }}</td>
          <td class="cell-name">{{ c.skillName || ('#' + c.skillId) }}</td>
          <td>{{ c.username || ('#' + c.userId) }}</td>
          <td class="cell-body">{{ c.body }}</td>
          <td>
            <span class="status-tag" :class="c.status === 'NORMAL' ? 'status-normal' : 'status-hidden'">
              {{ c.status === 'NORMAL' ? '正常' : '已隐藏' }}
            </span>
          </td>
          <td class="cell-time">{{ c.createTime || '—' }}</td>
          <td class="cell-actions">
            <a v-if="c.status === 'NORMAL'" @click.prevent="setStatus(c, 'HIDDEN')">隐藏</a>
            <a v-else @click.prevent="setStatus(c, 'NORMAL')">恢复</a>
            <a class="danger" @click.prevent="remove(c)">删除</a>
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

const loading = ref(true)
const items = ref<SkillComment[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('')
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

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
}
.cell-body {
  max-width: 360px;
  color: var(--muted);
  line-height: 1.6;
}
.cell-time {
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  white-space: nowrap;
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
.status-tag {
  font: 10px 'DM Mono', monospace;
  padding: 2px 7px;
  border: 1px solid var(--line);
  white-space: nowrap;
}
.status-normal {
  color: #68863d;
  border-color: #68863d;
}
.status-hidden {
  color: var(--muted);
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
