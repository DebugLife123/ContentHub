<template>
  <footer class="art-footer">
    <p class="art-end">本文完 · END</p>

    <!-- 行动区：收藏（真实 API）/ 分享（复制链接）/ 点赞数展示 -->
    <div class="art-footer-actions">
      <button
        class="art-action-btn"
        :class="{ 'is-active': article.favorited }"
        :disabled="favoriting"
        type="button"
        @click="$emit('toggle-favorite')"
      >
        {{ article.favorited ? '★ 已收藏' : '☆ 收藏' }} · {{ formatCount(article.collectCount) }}
      </button>
      <button class="art-action-btn" type="button" @click="$emit('share')">
        ↗ 分享文章
      </button>
      <span class="art-action-btn" aria-hidden="true">♡ {{ formatCount(article.likeCount) }} 点赞</span>
    </div>

    <div v-if="article.tags.length" class="art-tags">
      <RouterLink
        v-for="tag in article.tags"
        :key="tag"
        class="art-tag"
        :to="{ path: '/contents', query: { keyword: tag } }"
      ># {{ tag }}</RouterLink>
    </div>

    <!-- 上一篇 / 下一篇 -->
    <nav v-if="prev || next" class="art-prevnext" aria-label="文章导航">
      <RouterLink v-if="prev" class="art-prevnext-item" :to="`/content/${prev.id}`">
        <span>← 上一篇</span>
        <strong>{{ prev.title }}</strong>
      </RouterLink>
      <span v-else class="art-prevnext-item" />
      <RouterLink v-if="next" class="art-prevnext-item art-prevnext-item--next" :to="`/content/${next.id}`">
        <span>下一篇 →</span>
        <strong>{{ next.title }}</strong>
      </RouterLink>
      <span v-else class="art-prevnext-item" />
    </nav>
  </footer>
</template>

<script setup lang="ts">
import type { Article, ArticleNavItem } from '@/types/article'
import { formatCount } from '@/utils/articleMapper'

defineProps<{
  article: Article
  prev?: ArticleNavItem | null
  next?: ArticleNavItem | null
  favoriting?: boolean
}>()

defineEmits<{
  (e: 'toggle-favorite'): void
  (e: 'share'): void
}>()
</script>
