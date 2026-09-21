<template>
  <div class="dashboard-page content-width">
    <div class="dashboard-head">
      <div>
        <p class="eyebrow">CREATOR STUDIO / OVERVIEW</p>
        <h1>你好，<em>{{ displayName }}。</em></h1>
      </div>
      <el-button class="button button-dark" @click="$router.push('/creator/contents/new')">
        发布新内容 <span>↗</span>
      </el-button>
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
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已下架" value="OFFLINE" />
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
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td class="cell-title">{{ item.title }}</td>
            <td>{{ item.categoryName || '—' }}</td>
            <td>{{ item.accessType === 'FREE' ? '免费' : '订阅' }}</td>
            <td>
              <span class="status-tag" :class="`status-${item.status.toLowerCase()}`">
                {{ statusLabel(item.status) }}
              </span>
            </td>
            <td>{{ item.viewCount || 0 }}</td>
            <td class="cell-time">{{ item.updateTime || item.createTime || '—' }}</td>
            <td class="cell-actions">
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
import { deleteContent, pageMyContents } from '@/api/content'
import { useUserStore } from '@/stores/user'
import type { ContentItem, ContentStatus } from '@/api/types'

const userStore = useUserStore()

const loading = ref(true)
const items = ref<ContentItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref<ContentStatus | ''>('')

const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '创作者'
)

/** 统计卡片改为基于真实数据计算，不再写死假数字 */
const metrics = computed(() => {
  const published = items.value.filter((i) => i.status === 'PUBLISHED')
  const views = items.value.reduce((sum, i) => sum + (i.viewCount || 0), 0)
  const likes = items.value.reduce((sum, i) => sum + (i.likeCount || 0), 0)
  const drafts = items.value.filter((i) => i.status === 'DRAFT')
  return [
    { label: '内容总数', value: String(total.value), change: `本页已发布 ${published.length} 篇` },
    { label: '阅读量', value: String(views), change: '本页内容合计' },
    { label: '点赞数', value: String(likes), change: '本页内容合计' },
    { label: '草稿', value: String(drafts.length), change: '本页待发布' },
  ]
})

const STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  PENDING: '待审核',
  PUBLISHED: '已发布',
  REJECTED: '已驳回',
  OFFLINE: '已下架',
}
function statusLabel(status: string) {
  return STATUS_LABELS[status] ?? status
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

async function handleDelete(item: ContentItem) {
  try {
    await ElMessageBox.confirm(`确定删除《${item.title}》吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // 用户取消
  }

  const res = await deleteContent(item.id)
  if (res.data.success) {
    ElMessage.success('已删除')
    load()
  } else {
    ElMessage.error(res.data.message || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
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
  max-width: 260px;
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
.status-published {
  color: #68863d;
  border-color: #68863d;
}
.status-draft {
  color: var(--muted);
}
.status-offline {
  color: #c54a32;
  border-color: #c54a32;
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 22px;
}
</style>
