<template>
  <div class="home-page">
    <section class="hero content-width">
      <div class="hero-copy reveal-up">
        <p class="eyebrow">A HOME FOR INDEPENDENT KNOWLEDGE</p>
        <h1>把你的好内容，<em>变成长期价值。</em></h1>
        <p class="hero-subtitle">ContentHub 连接认真创作的人与持续学习的人。发布、订阅、沉淀，让每一次分享都被看见。</p>
        <div class="hero-actions">
          <RouterLink class="button button-dark" :to="canCreate ? '/creator' : '/register'">
            {{ canCreate ? '创建你的内容空间' : '成为创作者' }} <span>↗</span>
          </RouterLink>
          <RouterLink class="text-link" to="/contents">浏览精选内容 <span>↓</span></RouterLink>
        </div>
      </div>
      <div class="hero-note reveal-up delay-1"><span class="note-index">01 — 2026</span><span>精选创作者<br>正在这里发生</span></div>
      <div class="hero-visual reveal-up delay-2">
        <div class="orbit orbit-one"></div>
        <div class="orbit orbit-two"></div>
        <div class="visual-card card-main">
          <div class="card-topline"><span>FIELD NOTE / 024</span><span>READ 08 MIN</span></div>
          <div class="card-art"><span>写给<br>长期主义者的<br><i>一封信</i></span></div>
          <div class="card-bottom"><span>BY LIN YU</span><span>ARTICLE</span></div>
        </div>
        <div class="floating-stamp">CURATED<br><strong>FOR</strong><br>CURIOUS<br>MINDS</div>
        <div class="visual-card card-small"><span>THE<br>CREATOR<br>ECONOMY</span><b>→</b></div>
      </div>
    </section>

    <section id="explore" class="content-width explore-section">
      <div class="section-heading">
        <div><p class="eyebrow">EXPLORE THE LIBRARY</p><h2>值得收藏的<br><em>内容切片</em></h2></div>
        <p class="heading-aside">从一篇文章到一套完整课程，<br>找到适合你当下的那一份知识。</p>
      </div>

      <!-- 分类来自 GET /api/categories -->
      <div class="category-row">
        <span :class="{ active: activeCategoryId === null }" @click="selectCategory(null)">全部内容</span>
        <span
          v-for="category in categories"
          :key="category.id"
          :class="{ active: activeCategoryId === category.id }"
          @click="selectCategory(category.id)"
        >
          {{ category.name }}
        </span>
      </div>

      <div v-if="loading" class="empty-state">正在加载内容库…</div>
      <div v-else-if="error" class="empty-state">{{ error }}</div>
      <div v-else-if="!cards.length" class="empty-state">该分类下还没有已发布内容。</div>
      <div v-else class="content-grid">
        <article
          v-for="item in cards"
          :key="item.id"
          class="content-card"
          @click="$router.push(`/content/${item.id}`)"
        >
          <div class="content-cover" :class="item.theme">
            <span class="cover-type">{{ item.type }}</span>
            <strong>{{ item.cover }}</strong>
            <span class="cover-arrow">↗</span>
          </div>
          <div class="content-meta"><span>{{ item.author }}</span><span>{{ item.reading }}</span></div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.summary }}</p>
          <div class="content-foot"><span>{{ item.price }}</span><span>♡ {{ item.likes }}</span></div>
        </article>
      </div>

      <div class="home-more">
        <RouterLink class="text-link" to="/contents">查看全部内容 <span>→</span></RouterLink>
      </div>
    </section>

    <section id="creators" class="creator-banner content-width">
      <div><p class="eyebrow">FOR CREATORS</p><h2>你的经验，<br><em>值得被订阅。</em></h2></div>
      <p>不只是发布内容，而是建立一个属于你的、可持续成长的知识空间。用订阅连接真正认可你的人。</p>
      <RouterLink class="circle-arrow" :to="canCreate ? '/creator' : '/register'">↗</RouterLink>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { pageContents } from '../api/content'
import { listCategories } from '../api/category'
import { useUserStore } from '@/stores/user'
import type { Category, ContentItem } from '../api/types'

interface ContentCard {
  id: number
  type: string
  cover: string
  theme: string
  author: string
  reading: string
  title: string
  summary: string
  price: string
  likes: string | number
}

const userStore = useUserStore()
const canCreate = computed(() => userStore.hasRole('CREATOR', 'ADMIN'))

const loading = ref(true)
const error = ref('')
const cards = ref<ContentCard[]>([])
const categories = ref<Category[]>([])
const activeCategoryId = ref<number | null>(null)

const themes = ['theme-orange', 'theme-lilac', 'theme-ink', 'theme-yellow']
const typeLabels: Record<string, string> = {
  ARTICLE: 'ARTICLE', TUTORIAL: 'TUTORIAL', EBOOK: 'EBOOK', VIDEO: 'VIDEO', PDF: 'PDF',
  CODE: 'CODE TEMPLATE', PROMPT: 'PROMPT KIT', DATASET: 'DATASET', COLUMN: 'SERIES',
}

function mapContent(item: ContentItem, index: number): ContentCard {
  return {
    id: item.id,
    type: typeLabels[item.contentType] || item.contentType,
    cover: (item.title || 'CONTENT').toUpperCase().split(' ').slice(0, 3).join('\n'),
    theme: themes[index % themes.length],
    author: `BY CREATOR #${item.creatorId}`,
    reading: `${item.viewCount || 0} 次阅读`,
    title: item.title,
    summary: item.summary || '一份正在持续更新的数字内容。',
    price: item.accessType === 'FREE' ? '免费阅读' : '订阅后阅读',
    likes: item.likeCount || 0,
  }
}

async function loadContents() {
  loading.value = true
  error.value = ''
  try {
    const res = await pageContents({ pageNum: 1, pageSize: 6, categoryId: activeCategoryId.value })
    if (!res.data.success) {
      throw new Error(res.data.message || '内容加载失败')
    }
    cards.value = res.data.data.list.map(mapContent)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '内容加载失败，请确认后端已启动。'
    cards.value = []
  } finally {
    loading.value = false
  }
}

function selectCategory(id: number | null) {
  activeCategoryId.value = id
  loadContents()
}

onMounted(async () => {
  try {
    const res = await listCategories()
    if (res.data.success) categories.value = res.data.data
  } catch {
    categories.value = []
  }
  await loadContents()
})
</script>

<style scoped>
.home-more {
  margin-top: 34px;
  text-align: right;
}
.category-row span {
  user-select: none;
}
</style>
