<template>
  <div class="home-page">
    <section class="hero content-width">
      <div class="hero-copy reveal-up"><p class="eyebrow">A HOME FOR INDEPENDENT KNOWLEDGE</p><h1>把你的好内容，<em>变成长期价值。</em></h1><p class="hero-subtitle">ContentHub 连接认真创作的人与持续学习的人。发布、订阅、沉淀，让每一次分享都被看见。</p><div class="hero-actions"><RouterLink class="button button-dark" to="/creator">创建你的内容空间 <span>↗</span></RouterLink><a class="text-link" href="#explore">浏览精选内容 <span>↓</span></a></div></div>
      <div class="hero-note reveal-up delay-1"><span class="note-index">01 — 2026</span><span>精选创作者<br>正在这里发生</span></div>
      <div class="hero-visual reveal-up delay-2"><div class="orbit orbit-one"></div><div class="orbit orbit-two"></div><div class="visual-card card-main"><div class="card-topline"><span>FIELD NOTE / 024</span><span>READ 08 MIN</span></div><div class="card-art"><span>写给<br>长期主义者的<br><i>一封信</i></span></div><div class="card-bottom"><span>BY LIN YU</span><span>ARTICLE</span></div></div><div class="floating-stamp">CURATED<br><strong>FOR</strong><br>CURIOUS<br>MINDS</div><div class="visual-card card-small"><span>THE<br>CREATOR<br>ECONOMY</span><b>→</b></div></div>
    </section>
    <section id="explore" class="content-width explore-section"><div class="section-heading"><div><p class="eyebrow">EXPLORE THE LIBRARY</p><h2>值得收藏的<br><em>内容切片</em></h2></div><p class="heading-aside">从一篇文章到一套完整课程，<br>找到适合你当下的那一份知识。</p></div><div class="category-row"><span class="active">全部内容</span><span>技术文章</span><span>系列教程</span><span>电子书</span><span>Prompt</span><span>代码模板</span><span>数据集</span></div><div v-if="loading" class="empty-state">正在加载内容库…</div><div v-else-if="!cards.length" class="empty-state">还没有已发布内容，去创作者工作台发布第一篇吧。</div><div v-else class="content-grid"><article v-for="item in cards" :key="item.id" class="content-card" @click="$router.push(`/content/${item.id}`)"><div class="content-cover" :class="item.theme"><span class="cover-type">{{ item.type }}</span><strong>{{ item.cover }}</strong><span class="cover-arrow">↗</span></div><div class="content-meta"><span>{{ item.author }}</span><span>{{ item.reading }}</span></div><h3>{{ item.title }}</h3><p>{{ item.summary }}</p><div class="content-foot"><span>{{ item.price }}</span><span>♡ {{ item.likes }}</span></div></article></div></section>
    <section id="creators" class="creator-banner content-width"><div><p class="eyebrow">FOR CREATORS</p><h2>你的经验，<br><em>值得被订阅。</em></h2></div><p>不只是发布内容，而是建立一个属于你的、可持续成长的知识空间。用订阅连接真正认可你的人。</p><RouterLink class="circle-arrow" to="/creator">↗</RouterLink></section>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listContents, type ContentItem } from '../api/content'

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

const loading = ref(true)
const cards = ref<ContentCard[]>([])
const themes = ['theme-orange', 'theme-lilac', 'theme-ink', 'theme-yellow']
const typeLabels: Record<string, string> = { ARTICLE: 'ARTICLE', TUTORIAL: 'TUTORIAL', EBOOK: 'EBOOK', VIDEO: 'VIDEO', PDF: 'PDF', CODE: 'CODE TEMPLATE', PROMPT: 'PROMPT KIT', DATASET: 'DATASET', COLUMN: 'SERIES' }
const fallback: ContentCard[] = [{ id: 1, type: 'SERIES / 06 PARTS', cover: 'BUILD\nIN PUBLIC', theme: 'theme-orange', author: 'BY MIAO', reading: '更新于 2 天前', title: '一个独立开发者的产品实验室', summary: '从想法到上线，记录每一次真实的产品决策。', price: '订阅后阅读', likes: '128' }, { id: 2, type: 'PROMPT KIT', cover: 'PROMPT\nATLAS', theme: 'theme-lilac', author: 'BY KAI ZHOU', reading: '42 个模板', title: 'AI 工作流 Prompt 图鉴', summary: '把重复工作交给 AI，把时间还给真正重要的事。', price: '订阅后阅读', likes: '86' }, { id: 3, type: 'CODE TEMPLATE', cover: 'SHIP\nFASTER', theme: 'theme-ink', author: 'BY JUNE', reading: 'Vue 3 + Spring Boot', title: '全栈项目启动模板 2.0', summary: '开箱即用的工程底座，专为快速验证想法而生。', price: '免费阅读', likes: '214' }]

function mapContent(item: ContentItem, index: number): ContentCard {
  return { id: item.id, type: typeLabels[item.contentType] || item.contentType, cover: (item.title || 'CONTENT').toUpperCase().split(' ').slice(0, 3).join('\n'), theme: themes[index % themes.length], author: `BY CREATOR #${item.creatorId}`, reading: `${item.viewCount || 0} 次阅读`, title: item.title, summary: item.summary || '一份正在持续更新的数字内容。', price: item.accessType === 'FREE' ? '免费阅读' : '订阅后阅读', likes: item.likeCount || 0 }
}

onMounted(async () => {
  try {
    const response = await listContents()
    cards.value = response.data.success ? response.data.data.map(mapContent) : fallback
  } catch {
    cards.value = fallback
  } finally {
    loading.value = false
  }
})
</script>
