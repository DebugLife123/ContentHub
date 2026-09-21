<template>
  <div v-if="loading" class="art-page">
    <div class="empty-state">正在加载内容…</div>
  </div>

  <div v-else-if="error" class="art-page">
    <div class="empty-state">{{ error }}</div>
  </div>

  <ArticleLayout v-else>
    <!-- 左侧：目录 -->
    <template #toc>
      <ArticleToc :toc="article.toc" />
    </template>

    <!-- 中央：文章 -->
    <ArticleHeader :article="article" :status-label="statusLabel" />

    <p v-if="content.rejectReason" class="art-reject-line">
      审核驳回原因：{{ content.rejectReason }}
    </p>

    <!-- 未解锁：试读片段 + 订阅引导 -->
    <template v-if="content.locked">
      <div class="art-locked-banner">
        <strong>这份内容需要订阅后阅读</strong>
        <p>{{ content.lockReason }}</p>
      </div>
      <ArticleBody :blocks="previewBlocks" preview />
      <div class="art-lock-actions">
        <el-button class="button button-dark" @click="$router.push('/plans')">
          查看订阅方案 <span>↗</span>
        </el-button>
        <span class="art-lock-note">订阅后可阅读正文并获取附件地址</span>
      </div>
    </template>

    <!-- 已解锁：完整结构化正文 -->
    <template v-else>
      <ArticleBody :blocks="article.blocks" />
      <ArticleFooter
        :article="article"
        :prev="prevArticle"
        :next="nextArticle"
        :favoriting="favoriting"
        @toggle-favorite="toggleFavorite"
        @share="share"
      />
      <ArticleComments :content-id="article.id" />
    </template>

    <!-- 右侧：信息栏 -->
    <template #aside>
      <ArticleSidebar :article="article" :related="related" />
    </template>
  </ArticleLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  favoriteContent, getContent, hotContents, pageContents,
  unfavoriteContent, updateReadProgress,
} from '@/api/content'
import { useUserStore } from '@/stores/user'
import type { ContentItem } from '@/api/types'
import type { Article, ArticleNavItem } from '@/types/article'
import { CONTENT_STATUS_LABEL, toArticle } from '@/utils/articleMapper'
import { parseArticleBody, truncateBlocks } from '@/utils/articleParser'
import ArticleBody from '@/components/article/ArticleBody.vue'
import ArticleComments from '@/components/article/ArticleComments.vue'
import ArticleFooter from '@/components/article/ArticleFooter.vue'
import ArticleHeader from '@/components/article/ArticleHeader.vue'
import ArticleLayout from '@/components/article/ArticleLayout.vue'
import ArticleSidebar from '@/components/article/ArticleSidebar.vue'
import ArticleToc from '@/components/article/ArticleToc.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const error = ref('')
const favoriting = ref(false)

const content = ref<ContentItem>({
  id: 0,
  creatorId: 0,
  title: '',
  contentType: '',
  accessType: '',
  status: 'DRAFT',
})

const related = ref<ArticleNavItem[]>([])
const prevArticle = ref<ArticleNavItem | null>(null)
const nextArticle = ref<ArticleNavItem | null>(null)

/** 视图模型：解析正文 + 目录 + 阅读时长 */
const article = computed<Article>(() => toArticle(content.value))

/** 未解锁时解析试读片段，最多展示 6 个块 */
const previewBlocks = computed(() =>
  truncateBlocks(parseArticleBody(content.value.bodyPreview), 6),
)

const statusLabel = computed(() => {
  const status = content.value.status
  return status && status !== 'PUBLISHED' ? CONTENT_STATUS_LABEL[status] ?? status : ''
})

// ------------------------------------------------------------------ 数据加载

async function load(id: string | number) {
  loading.value = true
  error.value = ''
  related.value = []
  prevArticle.value = null
  nextArticle.value = null
  try {
    const response = await getContent(id)
    if (!response.data.success) throw new Error(response.data.message || '内容不存在')
    content.value = response.data.data
    void loadRelated()
    void loadPrevNext()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '内容加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

/** 相关阅读：复用热门内容接口，剔除当前篇 */
async function loadRelated() {
  try {
    const res = await hotContents(6)
    if (!res.data.success) return
    related.value = res.data.data
      .filter((item) => item.id !== content.value.id)
      .slice(0, 4)
      .map((item) => ({
        id: item.id,
        title: item.title,
        contentType: item.contentType,
        viewCount: item.viewCount ?? 0,
      }))
  } catch {
    related.value = []
  }
}

/** 上一篇 / 下一篇：取同分类列表，按返回顺序找邻居（失败则静默隐藏） */
async function loadPrevNext() {
  const categoryId = content.value.categoryId
  if (!categoryId) return
  try {
    const res = await pageContents({ categoryId, pageNum: 1, pageSize: 50 })
    if (!res.data.success) return
    const list = res.data.data.list
    const index = list.findIndex((item) => item.id === content.value.id)
    if (index === -1) return
    const toNav = (item: ContentItem): ArticleNavItem => ({ id: item.id, title: item.title })
    prevArticle.value = index > 0 ? toNav(list[index - 1]) : null
    nextArticle.value = index < list.length - 1 ? toNav(list[index + 1]) : null
  } catch {
    /* 导航加载失败不影响阅读 */
  }
}

// ------------------------------------------------------------------ 互动

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
      // 本地同步状态，避免整页重载打断阅读
      content.value.favorited = !content.value.favorited
      content.value.favoriteCount = (content.value.favoriteCount ?? 0) + (content.value.favorited ? 1 : -1)
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

async function share() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    ElMessage.success('链接已复制，快分享给朋友吧')
  } catch {
    ElMessage.warning('复制失败，请手动复制地址栏链接')
  }
}

// ------------------------------------------------------------------ 阅读进度

let lastReported = -1
let reportTimer: number | undefined

/**
 * 按滚动位置估算阅读进度并回传。
 * 只对已解锁且登录的读者上报；节流到 3 秒一次。
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

// ------------------------------------------------------------------ 生命周期

onMounted(() => {
  void load(route.params.id as string)
  window.addEventListener('scroll', handleScroll, { passive: true })
})

// 上一篇 / 下一篇 / 相关阅读 跳转的是同一路由组件，需要监听参数变化重载
watch(() => route.params.id, (id, oldId) => {
  if (id && id !== oldId) {
    lastReported = -1
    window.scrollTo({ top: 0 })
    void load(id as string)
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
/* 仅保留路由级零散样式；排版规范全部在 styles/article-system.scss */
.art-header {
  margin-bottom: var(--art-gap-xl);
}

.art-reject-line {
  margin: 0 0 var(--art-gap-md);
  font-size: 13px;
  color: var(--art-danger);
}

.empty-state {
  padding: 120px 0;
  text-align: center;
  color: var(--muted);
  font-size: 14px;
}
</style>
