<template>
  <div class="admin-page content-width">
    <div class="section-heading">
      <div><p class="eyebrow">ADMIN / CATEGORIES</p><h2>分类管理<br><em>内容分类维护</em></h2></div>
      <p class="heading-aside">仅管理员可见。<br>分类下有内容时不允许删除。</p>
    </div>

    <div class="toolbar">
      <el-button class="button button-dark" @click="openCreate">新增分类 <span>↗</span></el-button>
      <span v-if="message" :class="messageType === 'error' ? 'error-text' : 'success-text'">{{ message }}</span>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <table v-else class="admin-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>名称</th>
          <th>排序</th>
          <th>状态</th>
          <th>内容数</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>{{ item.id }}</td>
          <td class="cell-name">{{ item.name }}</td>
          <td>{{ item.sort }}</td>
          <td>
            <span class="status-tag" :class="item.status === 'ENABLED' ? 'status-on' : 'status-off'">
              {{ item.status === 'ENABLED' ? '启用' : '禁用' }}
            </span>
          </td>
          <td>{{ item.contentCount ?? 0 }}</td>
          <td class="cell-time">{{ item.createTime || '—' }}</td>
          <td class="cell-actions">
            <a @click.prevent="openEdit(item)">编辑</a>
            <a class="danger" @click.prevent="handleDelete(item)">删除</a>
          </td>
        </tr>
      </tbody>
    </table>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑分类' : '新增分类'" width="420px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" maxlength="60" placeholder="如：前端工程" />
        </el-form-item>
        <el-form-item label="排序值（越小越靠前）">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button class="button button-dark" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createCategory, deleteCategory, listAllCategories, updateCategory } from '@/api/category'
import type { Category } from '@/api/types'

const loading = ref(true)
const saving = ref(false)
const items = ref<Category[]>([])
const dialogVisible = ref(false)
const editing = ref<Category | null>(null)
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

const formRef = ref<FormInstance>()
const form = reactive({ name: '', sort: 0, status: 'ENABLED' as 'ENABLED' | 'DISABLED' })

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

function flash(text: string, type: 'success' | 'error' = 'success') {
  message.value = text
  messageType.value = type
  setTimeout(() => (message.value = ''), 3000)
}

async function load() {
  loading.value = true
  try {
    // 管理页要看到禁用中的分类，所以用 /categories/all 而不是公开的 /categories
    const res = await listAllCategories()
    if (res.data.success) items.value = res.data.data
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.name = ''
  form.sort = 0
  form.status = 'ENABLED'
  dialogVisible.value = true
}

function openEdit(item: Category) {
  editing.value = item
  form.name = item.name
  form.sort = item.sort
  form.status = item.status
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const res = editing.value
      ? await updateCategory(editing.value.id, { ...form })
      : await createCategory({ ...form })

    if (res.data.success) {
      dialogVisible.value = false
      flash(editing.value ? '已保存' : '已新增')
      await load()
    } else {
      flash(res.data.message || '保存失败', 'error')
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    flash(err.response?.data?.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function handleDelete(item: Category) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${item.name}」吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    const res = await deleteCategory(item.id)
    if (res.data.success) {
      flash('已删除')
      await load()
    } else {
      flash(res.data.message || '删除失败', 'error')
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    flash(err.response?.data?.message || '删除失败', 'error')
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
  align-items: center;
  gap: 18px;
  padding: 20px 0 8px;
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
}
.cell-name {
  font-weight: 600;
}
.cell-time {
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
}
.cell-actions a {
  margin-right: 12px;
  cursor: pointer;
  text-decoration: underline;
}
.cell-actions a.danger {
  color: #c54a32;
}
.status-tag {
  font: 10px 'DM Mono', monospace;
  padding: 2px 7px;
  border: 1px solid var(--line);
}
.status-on {
  color: #68863d;
  border-color: #68863d;
}
.status-off {
  color: var(--muted);
}
.success-text {
  color: #68863d;
  font-size: 12px;
}
</style>
