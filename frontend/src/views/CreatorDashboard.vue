<template>
  <div class="dashboard-page content-width">
    <div class="dashboard-head">
      <div>
        <p class="eyebrow">CREATOR STUDIO / OVERVIEW</p>
        <h1>你好，<em>{{ displayName }}。</em></h1>
      </div>
      <div class="head-actions">
        <el-button @click="$router.push('/creator/profile')">创作者资料</el-button>
        <el-button @click="$router.push('/creator/plans')">订阅套餐</el-button>
        <el-button @click="$router.push('/creator/revenue')">收益</el-button>
        <el-button class="button button-dark" @click="$router.push('/creator/contents/new')">
          发布新内容 <span>↗</span>
        </el-button>
      </div>
    </div>

    <div class="metric-grid">
      <div v-for="metric in metrics" :key="metric.label" class="metric-card">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.change }}</small>
      </div>
    </div>

    <section class="studio-panel">
      <div class="panel-head">
        <h2>我的内容</h2>
        <el-select
          v-model="statusFilter"
          placeholder="全部状态"
          clearable
          size="small"
          style="width: 150px"
          @change="applyFilter"
        >
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </div>

      <div v-if="loading" class="empty-state">正在加载…</div>
      <div v-else-if="!items.length" class="empty-state">
        还没有内容，点右上角「发布新内容」写下第一篇。
      </div>
      <table v-else class="content-table">
        <thead>
          <tr>
            <th>标题</th>
            <th>分类</th>
            <th>访问</th>
            <th>状态</th>
            <th>阅读</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td class="cell-title">
              {{ item.title }}
              <small v-if="item.rejectReason" class="reject-hint">驳回：{{ item.rejectReason }}</small>
            </td>
            <td>{{ item.categoryName || '—' }}</td>
            <td>{{ item.accessType === 'FREE' ? '免费' : '订阅' }}</td>
            <td>
              <span class="status-tag" :class="`status-${item.status.toLowerCase()}`">
                {{ statusLabel(item.status) }}
              </span>
            </td>
            <td>{{ item.viewCount || 0 }}</td>
            <td class="cell-actions">
              <!-- 状态流转按计划 Day 21 的规则给出可用操作 -->
              <a v-if="canSubmit(item.status)" @click.prevent="doSubmit(item)">提交审核</a>
              <a v-if="item.status === 'PUBLISHED'" @click.prevent="doOffline(item)">下架</a>
              <a @click.prevent="$router.push(`/creator/contents/${item.id}/edit`)">编辑</a>
              <a class="danger" @click.prevent="handleDelete(item)">删除</a>
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
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteContent, offlineContent, pageMyContents, submitContent,
} from '@/api/content'
import { getCreatorDashboard } from '@/api/creator'
import { useUserStore } from '@/stores/user'
import type { ContentItem, ContentStatus, CreatorDashboard } from '@/api/types'

const userStore = useUserStore()

const loading = ref(true)
const items = ref<ContentItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref<ContentStatus | ''>('')
/** 阶段 6 Day 48：统计来自 /creator/dashboard，而不是只统计当前页 */
const dashboard = ref<CreatorDashboard | null>(null)

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已下架', value: 'OFFLINE' },
]

const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '创作者'
)

const metrics = computed(() => {
  const d = dashboard.value
  if (!d) {
    // 仪表盘接口还没回来时先用本页数据兜底，避免卡片闪空
    const count = (s: string) => items.value.filter((i) => i.status === s).length
    return [
      { label: '内容总数', value: String(total.value), change: `本页已发布 ${count('PUBLISHED')} 篇` },
      { label: '待审核', value: String(count('PENDING')), change: '等待管理员处理' },
      { label: '草稿 / 已驳回', value: `${count('DRAFT')} / ${count('REJECTED')}`, change: '需要继续完善' },
      { label: '阅读量', value: String(items.value.reduce((s, i) => s + (i.viewCount || 0), 0)), change: '本页内容合计' },
    ]
  }
  return [
    { label: '内容总数', value: String(d.contentCount), change: `已发布 ${d.publishedCount} / 待审核 ${d.pendingCount}` },
    { label: '总阅读量', value: String(d.totalViews), change: `收藏 ${d.totalFavorites} · 评论 ${d.totalComments}` },
    { label: '有效订阅者', value: String(d.subscriberCount), change: `套餐 ${d.planCount} 个` },
    { label: '草稿 / 已驳回', value: `${d.draftCount} / ${d.rejectedCount}`, change: `已下架 ${d.offlineCount}` },
  ]
})

const STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿', PENDING: '待审核', PUBLISHED: '已发布', REJECTED: '已驳回', OFFLINE: '已下架',
}
function statusLabel(status: string) {
  return STATUS_LABELS[status] ?? status
}
/** 与后端状态机一致：只有 DRAFT / REJECTED / OFFLINE 能提交审核 */
function canSubmit(status: ContentStatus) {
  return ['DRAFT', 'REJECTED', 'OFFLINE'].includes(status)
}

async function load() {
  loading.value = true
  try {
    const res = await pageMyContents({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: statusFilter.value || undefined,
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

async function doSubmit(item: ContentItem) {
  const res = await submitContent(item.id)
  if (res.data.success) {
    ElMessage.success('已提交审核，等待管理员处理')
    load()
  } else {
    ElMessage.error(res.data.message || '提交失败')
  }
}

async function doOffline(item: ContentItem) {
  const res = await offlineContent(item.id)
  if (res.data.success) {
    ElMessage.success('已下架')
    load()
  } else {
    ElMessage.error(res.data.message || '下架失败')
  }
}

async function handleDelete(item: ContentItem) {
  try {
    await ElMessageBox.confirm(`确定删除《${item.title}》吗？`, '删除确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
    })
  } catch {
    return
  }
  const res = await deleteContent(item.id)
  if (res.data.success) {
    ElMessage.success('已删除')
    load()
  } else {
    ElMessage.error(res.data.message || '删除失败')
  }
}

async function loadDashboard() {
  try {
    const res = await getCreatorDashboard()
    if (res.data.success) dashboard.value = res.data.data
  } catch {
    dashboard.value = null
  }
}

onMounted(async () => {
  await Promise.all([load(), loadDashboard()])
})
</script>

<style scoped>
.head-actions {
  display: flex;
  gap: 10px;
}
.studio-panel {
  border: 1px solid var(--line);
  padding: 25px;
}
.content-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 18px;
  font-size: 13px;
}
.content-table th {
  text-align: left;
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  padding: 10px 8px;
  border-bottom: 1px solid var(--line);
}
.content-table td {
  padding: 13px 8px;
  border-bottom: 1px solid var(--line);
  vertical-align: middle;
}
.cell-title {
  max-width: 280px;
  font-weight: 600;
}
.reject-hint {
  display: block;
  margin-top: 5px;
  color: #c54a32;
  font: 10px 'DM Mono', monospace;
  font-weight: 400;
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
.status-published { color: #68863d; border-color: #68863d; }
.status-pending { color: #c07a1f; border-color: #c07a1f; }
.status-rejected { color: #c54a32; border-color: #c54a32; }
.status-offline { color: #c54a32; border-color: #c54a32; }
.status-draft { color: var(--muted); }
.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 22px;
}
</style>
