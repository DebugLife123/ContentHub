<template>
  <div class="review-page content-width">
    <div class="section-heading">
      <div><p class="eyebrow">ADMIN / CONTENT REVIEW</p><h2>内容审核<br><em>通过或驳回</em></h2></div>
      <p class="heading-aside">
        仅管理员可见。<br>只有待审核的内容才能通过或驳回。
      </p>
    </div>

    <div class="toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable size="default" style="width: 160px" @change="applyFilter">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索标题" clearable style="width: 220px"
                @keyup.enter="applyFilter" @clear="applyFilter" />
      <el-button class="button button-dark" @click="applyFilter">筛选</el-button>
      <span v-if="message" :class="messageType === 'error' ? 'error-text' : 'success-text'">{{ message }}</span>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <div v-else-if="!items.length" class="empty-state">没有符合条件的内容。</div>
    <table v-else class="review-table">
      <thead>
        <tr><th>标题</th><th>作者</th><th>分类</th><th>访问</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td class="cell-title">{{ item.title }}</td>
          <td>#{{ item.creatorId }}</td>
          <td>{{ item.categoryName || '—' }}</td>
          <td>{{ item.accessType === 'FREE' ? '免费' : '订阅' }}</td>
          <td>
            <span class="status-tag" :class="`status-${item.status.toLowerCase()}`">
              {{ statusLabel(item.status) }}
            </span>
          </td>
          <td class="cell-actions">
            <template v-if="item.status === 'PENDING'">
              <a @click.prevent="doApprove(item)">通过</a>
              <a class="danger" @click.prevent="openReject(item)">驳回</a>
            </template>
            <a v-else @click.prevent="preview(item)">查看</a>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="pager">
      <el-pagination layout="prev, pager, next, total" :total="total" :current-page="pageNum"
                     :page-size="pageSize" background @current-change="handlePageChange" />
    </div>

    <!-- 驳回原因 -->
    <el-dialog v-model="rejectVisible" title="驳回内容" width="440px">
      <p class="dialog-tip">驳回原因会展示给创作者，请写清楚需要修改什么。</p>
      <el-input v-model="rejectReason" type="textarea" :rows="4" maxlength="500" show-word-limit
                placeholder="如：缺少示例代码" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button class="button button-dark" :loading="saving" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 内容预览抽屉：排版渲染与读者端一致，可切换查看源文 -->
    <el-drawer v-model="drawerVisible" title="内容详情" size="52%">
      <div v-if="current" class="drawer-body">
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
        <pre v-else class="drawer-text">{{ current.body || current.bodyPreview || '（无正文）' }}</pre>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { approveContent, pageForReview, rejectContent } from '@/api/content'
import type { ContentItem, ContentStatus } from '@/api/types'
import { parseArticleBody } from '@/utils/articleParser'
import ArticleBody from '@/components/article/ArticleBody.vue'

const loading = ref(true)
const saving = ref(false)
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
  }
}

function openReject(item: ContentItem) {
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

function preview(item: ContentItem) {
  current.value = item
  drawerMode.value = 'preview'
  drawerVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.review-page { padding: 60px 0 30px; }
.toolbar { display: flex; align-items: center; gap: 12px; padding: 20px 0 8px; flex-wrap: wrap; }
.review-table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 12px; }
.review-table th {
  text-align: left; font: 10px 'DM Mono', monospace; color: var(--muted);
  padding: 10px 8px; border-bottom: 1px solid var(--line);
}
.review-table td { padding: 13px 8px; border-bottom: 1px solid var(--line); }
.cell-title { max-width: 300px; font-weight: 600; }
.cell-actions a { margin-right: 12px; cursor: pointer; text-decoration: underline; }
.cell-actions a.danger { color: #c54a32; }
.status-tag { font: 10px 'DM Mono', monospace; padding: 2px 7px; border: 1px solid var(--line); }
.status-published { color: #68863d; border-color: #68863d; }
.status-pending { color: #c07a1f; border-color: #c07a1f; }
.status-rejected { color: #c54a32; border-color: #c54a32; }
.status-offline { color: #c54a32; border-color: #c54a32; }
.status-draft { color: var(--muted); }
.pager { display: flex; justify-content: flex-end; padding-top: 22px; }
.dialog-tip { color: var(--muted); font-size: 12px; margin: 0 0 12px; }
.drawer-body h3 { margin: 0 0 8px; }
.drawer-meta { font: 10px 'DM Mono', monospace; color: var(--muted); }
.drawer-toggle { margin-left: 14px; }
.drawer-toggle a { cursor: pointer; margin-right: 10px; color: var(--muted); }
.drawer-toggle a.active { color: var(--ink); border-bottom: 1px solid var(--ink); padding-bottom: 2px; }
.drawer-summary { color: var(--muted); }
.drawer-text {
  white-space: pre-wrap; word-break: break-word; font-family: 'Noto Serif SC', serif;
  font-size: 14px; line-height: 1.9; background: var(--paper); padding: 16px; border: 1px solid var(--line);
}
.success-text { color: #68863d; font-size: 12px; }
</style>
