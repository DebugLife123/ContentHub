<template>
  <div>
    <AdminPageHeader
      eyebrow="ADMIN / SKILL MARKETPLACE"
      title="Skill 商城管理"
      accent="上架、下架与分类维护"
      :description="'共 ' + total + ' 个 Skill。这里改的数据会直接反映到前台 Skill 商城。'"
    >
      <template #actions>
        <el-button class="button button-dark" @click="$router.push('/admin/skills/new')">新增 Skill <span>↗</span></el-button>
      </template>
    </AdminPageHeader>

    <div class="tabs">
      <span :class="{ active: tab === 'skills' }" @click="tab = 'skills'">Skill 管理</span>
      <span :class="{ active: tab === 'categories' }" @click="tab = 'categories'">分类管理</span>
    </div>

    <!-- ---------------------------------------------------------- Skill 列表 -->
    <template v-if="tab === 'skills'">
      <div class="admin-toolbar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索名称 / 作者 / 仓库 / 标签"
          clearable
          class="filter-keyword"
          @keyup.enter="applyFilters"
          @clear="applyFilters"
        />
        <el-select v-model="filters.status" class="filter-select" @change="applyFilters">
          <el-option label="全部状态" value="" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已上架" value="PUBLISHED" />
          <el-option label="已下架" value="OFFLINE" />
        </el-select>
        <el-select v-model="filters.categoryId" placeholder="全部分类" clearable class="filter-select" @change="applyFilters">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button class="button button-dark" @click="applyFilters">筛选</el-button>
        <span v-if="message" class="admin-flash" :class="messageType === 'error' ? 'is-error' : 'is-ok'">{{ message }}</span>
      </div>

      <p v-if="loading" class="admin-loading">LOADING…</p>
      <div v-else-if="!items.length" class="admin-empty">
        <span class="admin-empty-mark">···</span>
        <p>没有符合条件的 Skill。</p>
      </div>
      <table v-else class="admin-table">
        <thead>
          <tr>
            <th>Skill</th>
            <th>分类</th>
            <th>星数</th>
            <th>类型</th>
            <th>状态</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td>
              <div class="cell-skill">
                <span class="cell-icon">{{ item.icon || '🧩' }}</span>
                <div>
                  <strong class="admin-cell-strong">{{ item.name }}</strong>
                  <small>{{ item.repo || '—' }}</small>
                </div>
              </div>
            </td>
            <td class="admin-cell-muted">{{ item.categoryName || '—' }}</td>
            <td class="admin-cell-mono">★ {{ formatStars(item.stars) }}</td>
            <td>
              <span class="admin-tag" :class="item.accessType === 'FREE' ? 'is-info' : 'is-warn'">
                {{ item.accessType === 'FREE' ? '免费' : '会员' }}
              </span>
            </td>
            <td>
              <span class="admin-tag" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
            </td>
            <td class="admin-cell-mono">{{ formatDate(item.updateTime) }}</td>
            <td class="admin-actions">
              <a @click.prevent="$router.push(`/admin/skills/${item.id}/edit`)">编辑</a>
              <a v-if="item.status !== 'PUBLISHED'" @click.prevent="handlePublish(item)">上架</a>
              <a v-else @click.prevent="handleOffline(item)">下架</a>
              <a class="is-danger" @click.prevent="handleDelete(item)">删除</a>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="admin-pager">
        <el-pagination
          layout="prev, pager, next, total"
          :total="total"
          :current-page="filters.pageNum"
          :page-size="filters.pageSize"
          background
          @current-change="handlePageChange"
        />
      </div>
    </template>

    <!-- ---------------------------------------------------------- 分类管理 -->
    <template v-else>
      <div class="admin-toolbar">
        <el-button class="button button-dark" @click="openCreateCategory">新增分类 <span>↗</span></el-button>
        <span v-if="message" class="admin-flash" :class="messageType === 'error' ? 'is-error' : 'is-ok'">{{ message }}</span>
      </div>

      <table class="admin-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>名称</th>
            <th>排序</th>
            <th>状态</th>
            <th>Skill 数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in allCategories" :key="c.id">
            <td class="admin-cell-muted">{{ c.id }}</td>
            <td class="admin-cell-strong">{{ c.name }}</td>
            <td class="admin-cell-muted">{{ c.sort }}</td>
            <td>
              <span class="admin-tag" :class="c.status === 'ENABLED' ? 'is-on' : 'is-off'">
                {{ c.status === 'ENABLED' ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="admin-cell-muted">{{ c.skillCount ?? 0 }}</td>
            <td class="admin-actions">
              <a @click.prevent="openEditCategory(c)">编辑</a>
              <a class="is-danger" @click.prevent="handleDeleteCategory(c)">删除</a>
            </td>
          </tr>
        </tbody>
      </table>
    </template>

    <el-dialog v-model="categoryDialog" class="admin-dialog" :title="editingCategory ? '编辑分类' : '新增分类'" width="420px">
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-position="top">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" maxlength="60" placeholder="如：开发工具" />
        </el-form-item>
        <el-form-item label="排序值（越小越靠前）">
          <el-input-number v-model="categoryForm.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="categoryForm.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialog = false">取消</el-button>
        <el-button class="button button-dark" :loading="savingCategory" @click="submitCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createSkillCategory,
  deleteSkill,
  deleteSkillCategory,
  formatDate,
  formatStars,
  listAllSkillCategories,
  offlineSkill,
  pageSkillsForAdmin,
  publishSkill,
  updateSkillCategory,
} from '@/api/skill'
import type { SkillCategory, SkillItem, SkillStatus } from '@/api/types'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

const tab = ref<'skills' | 'categories'>('skills')

const loading = ref(true)
const items = ref<SkillItem[]>([])
const total = ref(0)
const categories = ref<SkillCategory[]>([])
const allCategories = ref<SkillCategory[]>([])

const message = ref('')
const messageType = ref<'success' | 'error'>('success')

const filters = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '' as SkillStatus | '',
  categoryId: null as number | null,
})

const STATUS_LABEL: Record<SkillStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已上架',
  OFFLINE: '已下架',
}
const statusLabel = (s: SkillStatus) => STATUS_LABEL[s] ?? s

/** 状态 → admin-tag 变体（在 admin-system.scss 里统一定义） */
const STATUS_TAG_CLASS: Record<string, string> = {
  PUBLISHED: 'is-on',
  DRAFT: '',
  OFFLINE: 'is-off',
}
const statusClass = (s: SkillStatus) => STATUS_TAG_CLASS[s] ?? ''

function flash(text: string, type: 'success' | 'error' = 'success') {
  message.value = text
  messageType.value = type
  setTimeout(() => (message.value = ''), 3000)
}

/** 后端错误统一从响应体的 message 取，前端 axios 拦截器已经把它提到了 error.message */
function errText(e: unknown, fallback: string) {
  const err = e as { message?: string; response?: { data?: { message?: string } } }
  return err.response?.data?.message || err.message || fallback
}

async function loadSkills() {
  loading.value = true
  try {
    const res = await pageSkillsForAdmin({
      pageNum: filters.pageNum,
      pageSize: filters.pageSize,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      categoryId: filters.categoryId,
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

async function loadCategories() {
  try {
    const res = await listAllSkillCategories()
    if (res.data.success) {
      allCategories.value = res.data.data
      // 筛选下拉只放启用中的
      categories.value = res.data.data.filter((c) => c.status === 'ENABLED')
    }
  } catch {
    allCategories.value = []
    categories.value = []
  }
}

function applyFilters() {
  filters.pageNum = 1
  loadSkills()
}

function handlePageChange(page: number) {
  filters.pageNum = page
  loadSkills()
}

async function handlePublish(item: SkillItem) {
  try {
    const res = await publishSkill(item.id)
    if (res.data.success) {
      flash('已上架')
      await loadSkills()
    } else {
      flash(res.data.message || '上架失败', 'error')
    }
  } catch (e) {
    flash(errText(e, '上架失败'), 'error')
  }
}

async function handleOffline(item: SkillItem) {
  try {
    const res = await offlineSkill(item.id)
    if (res.data.success) {
      flash('已下架，前台不再展示')
      await loadSkills()
    } else {
      flash(res.data.message || '下架失败', 'error')
    }
  } catch (e) {
    flash(errText(e, '下架失败'), 'error')
  }
}

async function handleDelete(item: SkillItem) {
  try {
    await ElMessageBox.confirm(`确定删除 Skill「${item.name}」吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    const res = await deleteSkill(item.id)
    if (res.data.success) {
      flash('已删除')
      await Promise.all([loadSkills(), loadCategories()])
    } else {
      flash(res.data.message || '删除失败', 'error')
    }
  } catch (e) {
    flash(errText(e, '删除失败'), 'error')
  }
}

// ---------------------------------------------------------------- 分类

const categoryDialog = ref(false)
const savingCategory = ref(false)
const editingCategory = ref<SkillCategory | null>(null)
const categoryFormRef = ref<FormInstance>()
const categoryForm = reactive({ name: '', sort: 0, status: 'ENABLED' as 'ENABLED' | 'DISABLED' })
const categoryRules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

function openCreateCategory() {
  editingCategory.value = null
  categoryForm.name = ''
  categoryForm.sort = 0
  categoryForm.status = 'ENABLED'
  categoryDialog.value = true
}

function openEditCategory(c: SkillCategory) {
  editingCategory.value = c
  categoryForm.name = c.name
  categoryForm.sort = c.sort
  categoryForm.status = c.status
  categoryDialog.value = true
}

async function submitCategory() {
  if (!categoryFormRef.value) return
  const valid = await categoryFormRef.value.validate().catch(() => false)
  if (!valid) return

  savingCategory.value = true
  try {
    const res = editingCategory.value
      ? await updateSkillCategory(editingCategory.value.id, { ...categoryForm })
      : await createSkillCategory({ ...categoryForm })
    if (res.data.success) {
      categoryDialog.value = false
      flash(editingCategory.value ? '已保存' : '已新增')
      await loadCategories()
    } else {
      flash(res.data.message || '保存失败', 'error')
    }
  } catch (e) {
    flash(errText(e, '保存失败'), 'error')
  } finally {
    savingCategory.value = false
  }
}

async function handleDeleteCategory(c: SkillCategory) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${c.name}」吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    const res = await deleteSkillCategory(c.id)
    if (res.data.success) {
      flash('已删除')
      await Promise.all([loadCategories(), loadSkills()])
    } else {
      flash(res.data.message || '删除失败', 'error')
    }
  } catch (e) {
    flash(errText(e, '删除失败'), 'error')
  }
}

onMounted(async () => {
  await loadCategories()
  await loadSkills()
})
</script>

<style scoped>
/* 通用样式（页头 / 表格 / 标签 / 工具条 / 空态）统一在 styles/admin-system.scss，
   这里只保留本页特有的 tab 切换与 Skill 单元格徽章。 */
.tabs {
  display: flex;
  gap: 30px;
  border-bottom: 1px solid var(--line);
  margin-top: 10px;
}
.tabs span {
  position: relative;
  padding: 16px 0;
  font-size: 14px;
  color: var(--muted);
  cursor: pointer;
  user-select: none;
}
.tabs span.active {
  color: var(--ink);
  font-weight: 600;
}
.tabs span.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: var(--ink);
}
.filter-keyword {
  width: 260px;
}
.filter-select {
  width: 150px;
}
.cell-skill {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cell-icon {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3e7cf;
}
.cell-skill strong {
  display: block;
  font-size: 13px;
  word-break: break-all;
}
.cell-skill small {
  display: block;
  margin-top: 3px;
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  word-break: break-all;
}
</style>
