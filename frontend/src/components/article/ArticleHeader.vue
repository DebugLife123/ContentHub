<template>
  <header class="art-header">
    <RouterLink :to="backTo" class="art-back">
      <span class="art-back-arrow">←</span> 返回内容库
    </RouterLink>

    <p class="art-eyebrow">
      <span class="art-eyebrow-item">{{ article.category }}</span>
      <span class="art-eyebrow-dot">/</span>
      <span class="art-eyebrow-item">{{ article.contentType }}</span>
      <span class="art-eyebrow-dot">/</span>
      <span class="art-badge" :class="{ 'art-badge--accent': article.accessType !== 'FREE' }">
        {{ article.accessType === 'FREE' ? '免费阅读' : '订阅专属' }}
      </span>
      <span v-if="statusLabel" class="art-badge">{{ statusLabel }}</span>
    </p>

    <h1 class="art-title">{{ article.title }}</h1>

    <p v-if="article.summary" class="art-lede">{{ article.summary }}</p>

    <ArticleMeta :article="article" />
  </header>
</template>

<script setup lang="ts">
import type { Article } from '@/types/article'
import ArticleMeta from './ArticleMeta.vue'

withDefaults(defineProps<{
  article: Article
  statusLabel?: string
  backTo?: string
}>(), {
  statusLabel: '',
  backTo: '/contents',
})
</script>
