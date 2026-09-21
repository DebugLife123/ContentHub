<template>
  <div class="detail-page content-width">
    <RouterLink to="/contents" class="back-link">← 返回内容库</RouterLink>

    <div v-if="loading" class="empty-state">正在加载内容…</div>
    <div v-else-if="error" class="empty-state">{{ error }}</div>
    <div v-else class="detail-layout">
      <article>
        <p class="eyebrow">
          {{ content.contentType }} · {{ content.categoryName || '未分类' }} ·
          {{ content.accessType === 'FREE' ? '免费阅读' : '订阅专属' }}
        </p>
        <h1>{{ content.title }}</h1>
        <p class="detail-lede">{{ content.summary }}</p>

        <!-- 未解锁：只给试读片段 + 订阅引导 -->
        <template v-if="content.locked">
          <div class="locked-banner">
            <strong>这份内容需要订阅后阅读</strong>
            <p>{{ content.lockReason }}</p>
          </div>
          <div class="article-body preview">
            <p>{{ content.bodyPreview }}</p>
          </div>
          <div class="lock-actions">
            <el-button class="button button-dark" @click="$router.push('/plans')">
              查看订阅方案 <span>↗</span>
            </el-button>
            <span class="aside-note">订阅后可阅读正文并获取附件地址</span>
          </div>
        </template>

        <!-- 已解锁：完整正文 -->
        <template v-else>
          <div class="article-body">
            <p v-for="(paragraph, index) in paragraphs" :key="index">{{ paragraph }}</p>
          </div>
          <p v-if="content.fileUrl" class="file-line">
            附件地址：<a :href="content.fileUrl" target="_blank" rel="noopener">{{ content.fileUrl }}</a>
          </p>
        </template>

        <p v-if="content.rejectReason" class="reject-line">
          审核驳回原因：{{ content.rejectReason }}
        </p>
      </article>

      <aside class="detail-aside">
        <div class="aside-cover theme-orange">
          CONTENT<br><strong>NOTE</strong><br><small>#{{ content.id }}</small>
        </div>
        <div class="author-box">
          <span class="avatar">C</span>
          <div>
            <strong>创作者 #{{ content.creatorId }}</strong>
            <small>数字内容创作者</small>
          </div>
        </div>

        <div class="stat-row">
          <span>{{ content.viewCount || 0 }} 次阅读</span>
          <span>♡ {{ content.likeCount || 0 }}</span>
          <span>★ {{ content.favoriteCount || 0 }}</span>
          <span>💬 {{ content.commentCount || 0 }}</span>
        </div>

        <el-button
          class="button button-dark full-button"
          :loading="favoriting"
          @click="toggleFavorite"
        >
          {{ content.favorited ? '已收藏，点击取消' : '收藏这份内容' }} <span>↗</span>
        </el-button>

        <p class="aside-note">
          {{ content.accessType === 'FREE' ? '这份内容对所有人开放。' : '订阅该创作者即可解锁全部订阅内容。' }}
        </p>
        <p v-if="content.hotScore" class="aside-note">热度分：{{ Number(content.hotScore).toFixed(1) }}</p>
      </aside>
    </div>

    <!-- 评论（阶段 5 Day 45） -->
    <section v-if="!loading && !error" class="comments-section">
      <div class="panel-head">
        <h2>评论 <small>{{ commentTotal }}</small></h2>
      </div>

      <div class="comment-form">
        <el-input v-model="newComment" type="textarea" :rows="3" maxlength="1000" show-word-limit
                  :placeholder="userStore.isLoggedIn ? '写下你的看法…' : '登录后即可评论'" />
        <el-button class="button button-dark" :loading="posting" @click="submitComment">
          {{ userStore.isLoggedIn ? '发表评论' : '登录后评论' }} <span>↗</span>
        </el-button>
      </div>

      <div v-if="!comments.length" class="empty-state">还没有评论，来做第一个。</div>
      <ul v-else class="comment-list">
        <li v-for="c in comments" :key="c.id">
          <div class="comment-head">
            <strong>{{ c.username || ('用户 #' + c.userId) }}</strong>
            <span>{{ c.createTime }}</span>
          </div>
          <p>{{ c.body }}</p>
          <a v-if="c.canDelete" class="comment-del" @click.prevent="removeComment(c)">删除</a>
        </li>
      </ul>

      <div v-if="commentTotal > comments.length" class="pager">
        <el-pagination layout="prev, pager, next" :total="commentTotal" :current-page="commentPage"
                       :page-size="commentPageSize" background @current-change="handleCommentPage" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createComment, deleteComment, favoriteContent, getContent, listComments,
  unfavoriteContent, updateReadProgress,
} from '../api/content'
import { useUserStore } from '@/stores/user'
import type { Comment, ContentItem } from '../api/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const error = ref('')
const favoriting = ref(false)
const posting = ref(false)

const comments = ref<Comment[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentPageSize = ref(5)
const newComment = ref('')

/** 空内容占位：模板中可直接访问字段，无需到处判空 */
const EMPTY_CONTENT: ContentItem = {
  id: 0,
  creatorId: 0,
  title: '',
  contentType: '',
  accessType: '',
  status: 'DRAFT',
}

const content = ref<ContentItem>(EMPTY_CONTENT)
const paragraphs = computed(() =>
  (content.value.body || '').split(/\n+/).filter(Boolean)
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await getContent(String(route.params.id))
    if (!response.data.success) throw new Error(response.data.message || '内容不存在')
    content.value = response.data.data
  } catch (e) {
    error.value = e instanceof Error ? e.message : '内容加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  favoriting.value = true
  try {
    const action = content.value.favorited ? unfavoriteContent : favoriteContent
    const res = await action(content.value.id)
    if (res.data.success) {
      ElMessage.success(content.value.favorited ? '已取消收藏' : '已收藏')
      await load()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    favoriting.value = false
  }
}

onMounted(async () => {
  await load()
  await loadComments()
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

// ------------------------------------------------ 评论（阶段 5 Day 45）

async function loadComments() {
  try {
    const res = await listComments(content.value.id, commentPage.value, commentPageSize.value)
    if (res.data.success) {
      comments.value = res.data.data.list
      commentTotal.value = res.data.data.total
    }
  } catch {
    comments.value = []
    commentTotal.value = 0
  }
}

function handleCommentPage(p: number) {
  commentPage.value = p
  loadComments()
}

async function submitComment() {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!newComment.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  posting.value = true
  try {
    const res = await createComment(content.value.id, newComment.value.trim())
    if (res.data.success) {
      newComment.value = ''
      commentPage.value = 1
      await loadComments()
      ElMessage.success('评论已发表')
    } else {
      ElMessage.error(res.data.message || '发表失败')
    }
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '发表失败')
  } finally {
    posting.value = false
  }
}

async function removeComment(c: Comment) {
  const res = await deleteComment(c.id)
  if (res.data.success) {
    ElMessage.success('已删除')
    await loadComments()
  } else {
    ElMessage.error(res.data.message || '删除失败')
  }
}

// ------------------------------------------------ 阅读进度（阶段 5 Day 46）

let lastReported = -1
let reportTimer: number | undefined

/**
 * 按滚动位置估算阅读进度并回传。
 *
 * 只对已解锁且登录的读者上报；节流到 3 秒一次，避免滚动时把接口打爆。
 */
function handleScroll() {
  if (!userStore.isLoggedIn || content.value.locked || !content.value.id) return
  if (reportTimer) return

  reportTimer = window.setTimeout(async () => {
    reportTimer = undefined
    const doc = document.documentElement
    const scrollable = doc.scrollHeight - doc.clientHeight
    const percent = scrollable <= 0
      ? 100
      : Math.min(100, Math.round((doc.scrollTop / scrollable) * 100))

    // 进度只上报增长，避免来回滚动时反复写库
    if (percent <= lastReported) return
    lastReported = percent
    try {
      await updateReadProgress(content.value.id, percent)
    } catch {
      // 进度上报失败不影响阅读
    }
  }, 3000)
}
</script>

<style scoped>
.locked-banner {
  border-left: 3px solid var(--orange);
  background: rgba(241, 125, 71, 0.08);
  padding: 18px 22px;
  margin: 30px 0 10px;
}
.locked-banner strong {
  display: block;
  margin-bottom: 6px;
}
.locked-banner p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}
.article-body.preview {
  color: var(--muted);
  -webkit-mask-image: linear-gradient(180deg, #000 55%, transparent 100%);
  mask-image: linear-gradient(180deg, #000 55%, transparent 100%);
}
.lock-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
}
.file-line {
  margin-top: 26px;
  font: 11px 'DM Mono', monospace;
  word-break: break-all;
}
.reject-line {
  margin-top: 20px;
  color: #c54a32;
  font-size: 13px;
}
.stat-row {
  display: flex;
  justify-content: space-between;
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  padding: 14px 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  margin: 16px 0;
}
.comments-section {
  margin-top: 60px;
  border-top: 1px solid var(--line);
  padding-top: 34px;
  max-width: 760px;
}
.comments-section .panel-head h2 {
  font-size: 22px;
  letter-spacing: -0.04em;
  margin: 0 0 20px;
}
.comments-section .panel-head small {
  font: 11px 'DM Mono', monospace;
  color: var(--muted);
  margin-left: 8px;
}
.comment-form {
  margin-bottom: 26px;
}
.comment-form .el-button {
  margin-top: 12px;
}
.comment-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.comment-list li {
  position: relative;
  border-bottom: 1px solid var(--line);
  padding: 16px 0;
}
.comment-head {
  display: flex;
  justify-content: space-between;
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
  margin-bottom: 8px;
}
.comment-list p {
  margin: 0;
  line-height: 1.8;
  font-size: 14px;
}
.comment-del {
  position: absolute;
  right: 0;
  bottom: 14px;
  font: 10px 'DM Mono', monospace;
  color: #c54a32;
  cursor: pointer;
  text-decoration: underline;
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
}
</style>
