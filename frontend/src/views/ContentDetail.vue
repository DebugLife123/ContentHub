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
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { favoriteContent, getContent, unfavoriteContent } from '../api/content'
import { useUserStore } from '@/stores/user'
import type { ContentItem } from '../api/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const error = ref('')
const favoriting = ref(false)

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

onMounted(load)
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
</style>
