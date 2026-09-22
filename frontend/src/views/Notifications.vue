<template>
  <div class="notify-page content-width">
    <div class="section-heading">
      <div>
        <p class="eyebrow">NOTIFICATIONS</p>
        <h2>站内通知<br /><em>审核结果与申请进度</em></h2>
      </div>
      <p class="heading-aside">
        未读 <strong>{{ unread }}</strong> 条<br />
        审核通过、驳回、创作者申请结果都会出现在这里。
      </p>
    </div>

    <div class="toolbar">
      <el-radio-group v-model="unreadOnly" @change="reload">
        <el-radio-button :value="false">全部</el-radio-button>
        <el-radio-button :value="true">只看未读</el-radio-button>
      </el-radio-group>
      <el-button class="button button-dark" :disabled="!unread" @click="readAll">全部标记已读</el-button>
    </div>

    <div v-if="loading" class="empty-state">正在加载…</div>
    <div v-else-if="!items.length" class="empty-state">
      {{ unreadOnly ? '没有未读通知。' : '还没有收到任何通知。' }}
    </div>
    <ul v-else class="notify-list">
      <li v-for="n in items" :key="n.id" :class="{ unread: !n.read }" @click="open(n)">
        <span class="dot" :class="{ on: !n.read }"></span>
        <div class="notify-main">
          <div class="notify-head">
            <strong>{{ n.title }}</strong>
            <span>{{ n.createTime }}</span>
          </div>
          <p v-if="n.body">{{ n.body }}</p>
        </div>
        <span class="notify-type">{{ typeLabel(n.type) }}</span>
      </li>
    </ul>

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
import { useRouter } from 'vue-router'
import { markAllNotificationsRead, markNotificationRead, myNotifications } from '@/api/notification'
import type { NotificationItem } from '@/api/notification'

const router = useRouter()

const loading = ref(true)
const items = ref<NotificationItem[]>([])
const total = ref(0)
const unread = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const unreadOnly = ref(false)

const TYPE_LABEL: Record<string, string> = {
  CONTENT_APPROVED: '内容通过',
  CONTENT_REJECTED: '内容驳回',
  CONTENT_OFFLINE: '内容下架',
  CREATOR_APPROVED: '申请通过',
  CREATOR_REJECTED: '申请驳回',
}
const typeLabel = (t: string) => TYPE_LABEL[t] ?? t

async function load() {
  loading.value = true
  try {
    const res = await myNotifications({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      unreadOnly: unreadOnly.value || undefined,
    })
    if (res.data.success) {
      items.value = res.data.data.list
      total.value = res.data.data.total
      unread.value = items.value.filter((n) => !n.read).length
    }
  } catch {
    items.value = []
    total.value = 0
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

/** 点通知先标记已读，再按 bizType 跳到对应页面 */
async function open(n: NotificationItem) {
  if (!n.read) {
    try {
      await markNotificationRead(n.id)
      n.read = true
      unread.value = Math.max(0, unread.value - 1)
    } catch { /* 标记失败不影响跳转 */ }
  }
  if (n.bizType === 'CONTENT' && n.bizId) {
    router.push(`/content/${n.bizId}`)
  } else if (n.bizType === 'CREATOR_APPLICATION') {
    router.push('/profile')
  }
}

async function readAll() {
  try {
    const res = await markAllNotificationsRead()
    if (res.data.success) await load()
  } catch { /* 忽略 */ }
}

onMounted(load)
</script>

<style scoped>
.notify-page {
  padding: 60px 0 30px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  padding: 20px 0 8px;
}
.notify-list {
  list-style: none;
  margin: 12px 0 0;
  padding: 0;
}
.notify-list li {
  display: grid;
  grid-template-columns: 12px 1fr auto;
  gap: 14px;
  align-items: start;
  padding: 18px 0;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
}
.notify-list li:hover {
  background: rgba(241, 125, 71, 0.04);
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: transparent;
  margin-top: 7px;
}
.dot.on {
  background: var(--orange);
}
.notify-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
.notify-head strong {
  font-size: 14px;
}
.notify-list li.unread .notify-head strong {
  font-weight: 700;
}
.notify-head span {
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  white-space: nowrap;
}
.notify-main p {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--muted);
}
.notify-type {
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  border: 1px solid var(--line);
  padding: 3px 8px;
  white-space: nowrap;
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding: 30px 0 10px;
}
</style>
