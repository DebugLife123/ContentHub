<template>
  <div>
    <AdminPageHeader
      eyebrow="ADMIN / CONTENT REVIEW"
      title="内容审核"
      accent="通过或驳回"
      description="仅管理员可见。只有待审核的内容才能通过或驳回；点「查看」可按读者端的排版预览正文。"
    />

    <div class="admin-toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable size="default" style="width: 160px" @change="applyFilter">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索标题" clearable style="width: 220px"
                @keyup.enter="applyFilter" @clear="applyFilter" />
      <el-button class="button button-dark" @click="applyFilter">筛选</el-button>
      <span v-if="message" class="admin-flash" :class="messageType === 'error' ? 'is-error' : 'is-ok'">{{ message }}</span>
    </div>

    <p v-if="loading" class="admin-loading">LOADING…</p>
    <div v-else-if="!items.length" class="admin-empty">
      <span class="admin-empty-mark">···</span>
      <p>没有符合条件的内容。</p>
    </div>
    <table v-else class="admin-table">
      <thead>
        <tr><th>标题</th><th>作者</th><th>分类</th><th>访问</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>
            <div class="admin-cell-strong admin-cell-clip">{{ item.title }}</div>
          </td>
          <td class="admin-cell-muted">#{{ item.creatorId }}</td>
          <td class="admin-cell-muted">{{ item.categoryName || '—' }}</td>
          <td>
            <span class="admin-tag" :class="item.accessType === 'FREE' ? 'is-info' : 'is-warn'">
              {{ item.accessType === 'FREE' ? '免费' : '订阅' }}
            </span>
          </td>
          <td>
            <span class="admin-tag" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
          </td>
          <td class="admin-actions">
            <!-- 查看必须始终可用：审核的第一动作是看正文，待审核行尤其需要 -->
            <a :class="{ 'is-busy': previewingId === item.id }" @click.prevent="preview(item)">查看</a>
            <template v-if="item.status === 'PENDING'">
              <a :class="{ 'is-busy': actingId === item.id }" @click.prevent="doApprove(item)">通过</a>
              <a class="is-danger" :class="{ 'is-busy': actingId === item.id }"
                 @click.prevent="openReject(item)">驳回</a>
            </template>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="admin-pager">
      <el-pagination layout="prev, pager, next, total" :total="total" :current-page="pageNum"
                     :page-size="pageSize" background @current-change="handlePageChange" />
    </div>

    <!-- 驳回原因 -->
    <el-dialog v-model="rejectVisible" class="admin-dialog" title="驳回内容" width="440px">
      <p class="dialog-tip">驳回原因会展示给创作者，请写清楚需要修改什么。</p>
      <el-input v-model="rejectReason" type="textarea" :rows="4" maxlength="500" show-word-limit
                placeholder="如：缺少示例代码" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button class="button button-dark" :loading="saving" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 内容预览抽屉：排版渲染与读者端一致，可切换查看源文 -->
    <el-drawer v-model="drawerVisible" class="admin-drawer" title="内容详情" size="52%">
      <p v-if="drawerLoading" class="admin-loading">LOADING…</p>
      <div v-else-if="current" class="drawer-body">
        <h3>{{ current.title }}</h3>
        <p class="drawer-meta">
          #{{ current.id }} · 创作者 #{{ current.creatorId }} · {{ current.categoryName || '未分类' }}
          <span class="drawer-toggle">
            <a :class="{ active: drawerMode === 'preview' }" @click.prevent="drawerMode = 'preview'">排版预览</a>
            <a :class="{ active: drawerMode === 'source' }" @click.prevent="drawerMode = 'source'">源文</a>
          </span>
        </p>
        <p class="drawer-summary">{{ current.summary }}</p>
        <ArticleBody v-if="drawerMode === 'preview'" :blocks="drawerBlocks" />
        <pre v-else class="drawer-text">{{ current.body || '（无正文）' }}</pre>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { approveContent, getAdminContent, pageForReview, rejectContent } from '@/api/content'
import type { ContentItem, ContentStatus } from '@/api/types'
import { parseArticleBody } from '@/utils/articleParser'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import ArticleBody from '@/components/article/ArticleBody.vue'

const loading = ref(true)
const saving = ref(false)
/** 正在审核的行 id：审核请求在途时禁用该行操作，避免连点发出两次审核 */
const actingId = ref<number | null>(null)
/** 正在拉取正文的行 id：抽屉打开前要先取回 body（列表接口不返回正文） */
const previewingId = ref<number | null>(null)
const drawerLoading = ref(false)
const items = ref<ContentItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref<ContentStatus | ''>('PENDING')
const keyword = ref('')
const message = ref('')
const messageType = ref<'success' | 'error'>('success')

const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectTarget = ref<ContentItem | null>(null)

const drawerVisible = ref(false)
const current = ref<ContentItem | null>(null)
const drawerMode = ref<'preview' | 'source'>('preview')

/** 审核看到的排版 = 读者看到的排版（同一条解析管线） */
const drawerBlocks = computed(() =>
  parseArticleBody(current.value?.body || current.value?.bodyPreview),
)

const statusOptions = [
  { label: '待审核', value: 'PENDING' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已下架', value: 'OFFLINE' },
]

const STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿', PENDING: '待审核', PUBLISHED: '已发布', REJECTED: '已驳回', OFFLINE: '已下架',
}
function statusLabel(s: string) {
  return STATUS_LABELS[s] ?? s
}

/** 状态 → admin-tag 变体（在 admin-system.scss 里统一定义） */
const STATUS_TAG_CLASS: Record<string, string> = {
  PUBLISHED: 'is-on',
  PENDING: 'is-warn',
  REJECTED: 'is-off',
  OFFLINE: 'is-off',
  DRAFT: '',
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
    const res = await pageForReview({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: status.value || undefined,
      keyword: keyword.value || undefined,
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
function handlePageChange(page: number) {
  pageNum.value = page
  load()
}

async function doApprove(item: ContentItem) {
  if (actingId.value !== null) return
  actingId.value = item.id
  try {
    const res = await approveContent(item.id)
    if (res.data.success) {
      flash('已通过，内容现在对读者可见')
      await load()
    } else {
      flash(res.data.message || '操作失败', 'error')
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    flash(err.response?.data?.message || '操作失败', 'error')
  } finally {
    actingId.value = null
  }
}

function openReject(item: ContentItem) {
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
    const res = await rejectContent(rejectTarget.value.id, rejectReason.value.trim())
    if (res.data.success) {
      rejectVisible.value = false
      flash('已驳回')
      await load()
    } else {
      flash(res.data.message || '操作失败', 'error')
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    flash(err.response?.data?.message || '操作失败', 'error')
  } finally {
    saving.value = false
  }
}

/**
 * 打开预览。
 *
 * 列表接口返回的 ContentListVO 不含 body，所以这里必须再拉一次审核详情，
 * 否则「查看」打开的是空正文——审核者最需要看的东西反而看不到。
 */
async function preview(item: ContentItem) {
  if (previewingId.value !== null) return
  previewingId.value = item.id
  drawerMode.value = 'preview'
  drawerVisible.value = true
  drawerLoading.value = true
  current.value = item
  try {
    const res = await getAdminContent(item.id)
    if (res.data.success) {
      current.value = res.data.data
    } else {
      flash(res.data.message || '加载正文失败', 'error')
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    flash(err.response?.data?.message || '加载正文失败', 'error')
  } finally {
    drawerLoading.value = false
    previewingId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
/* 通用样式（页头 / 表格 / 标签 / 工具条）统一在 styles/admin-system.scss，
   这里只保留本页特有的抽屉内容排版。 */
.dialog-tip { color: var(--muted); font-size: 12px; margin: 0 0 12px; }
.drawer-body h3 { margin: 0 0 8px; font-size: 19px; letter-spacing: -0.02em; }
.drawer-meta { font: 10px 'DM Mono', monospace; color: var(--muted); }
.drawer-toggle { margin-left: 14px; }
.drawer-toggle a { cursor: pointer; margin-right: 10px; color: var(--muted); }
.drawer-toggle a.active { color: var(--ink); border-bottom: 1px solid var(--ink); padding-bottom: 2px; }
.drawer-summary { color: var(--muted); }
.drawer-text {
  white-space: pre-wrap; word-break: break-word; font-family: 'Noto Serif SC', serif;
  font-size: 14px; line-height: 1.9; background: var(--paper); padding: 16px; border: 1px solid var(--line);
}
</style>
