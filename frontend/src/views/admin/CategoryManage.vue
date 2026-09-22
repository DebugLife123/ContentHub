<template>
  <div>
    <AdminPageHeader
      eyebrow="ADMIN / CATEGORIES"
      title="分类管理"
      accent="内容分类维护"
      description="仅管理员可见。分类下有内容时不允许删除。"
    >
      <template #actions>
        <el-button class="button button-dark" @click="openCreate">新增分类 <span>↗</span></el-button>
      </template>
    </AdminPageHeader>

    <!-- 没有筛选控件，只在有反馈文案时占一行，避免常态空出一段留白 -->
    <div v-if="message" class="admin-toolbar">
      <span class="admin-flash" :class="messageType === 'error' ? 'is-error' : 'is-ok'">{{ message }}</span>
    </div>

    <p v-if="loading" class="admin-loading">LOADING…</p>
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
          <td class="admin-cell-mono">{{ item.id }}</td>
          <td class="admin-cell-strong">{{ item.name }}</td>
          <td class="admin-cell-mono">{{ item.sort }}</td>
          <td>
            <span class="admin-tag" :class="statusClass(item.status)">{{ item.status === 'ENABLED' ? '启用' : '禁用' }}</span>
          </td>
          <td class="admin-cell-mono">{{ item.contentCount ?? 0 }}</td>
          <td class="admin-cell-mono">{{ item.createTime || '—' }}</td>
          <td class="admin-actions">
            <a @click.prevent="openEdit(item)">编辑</a>
            <a class="is-danger" @click.prevent="handleDelete(item)">删除</a>
          </td>
        </tr>
      </tbody>
    </table>

    <el-dialog v-model="dialogVisible" class="admin-dialog" :title="editing ? '编辑分类' : '新增分类'" width="420px">
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
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

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

/** 状态 → admin-tag 变体（在 admin-system.scss 里统一定义） */
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
