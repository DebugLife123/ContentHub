<template>
  <div class="art-meta">
    <span class="art-meta-author">
      <span class="art-avatar">
        <img v-if="article.author.avatar" :src="article.author.avatar" :alt="article.author.name">
        <template v-else>{{ initial }}</template>
      </span>
      {{ article.author.name }}
    </span>

    <span v-if="article.publishTime" class="art-meta-sep" />
    <span v-if="article.publishTime" class="art-meta-item">
      <i class="art-meta-icon">📅</i>{{ article.publishTime }}
    </span>

    <span v-if="article.updateTime && article.updateTime !== article.publishTime" class="art-meta-sep" />
    <span
      v-if="article.updateTime && article.updateTime !== article.publishTime"
      class="art-meta-item"
    >
      <i class="art-meta-icon">↻</i>更新于 {{ article.updateTime }}
    </span>

    <span class="art-meta-sep" />
    <span class="art-meta-item"><i class="art-meta-icon">◷</i>{{ article.readTime }} min read</span>

    <span class="art-meta-sep" />
    <span class="art-meta-item"><i class="art-meta-icon">◇</i>{{ views }} views</span>

    <span class="art-meta-sep" />
    <span class="art-meta-item"><i class="art-meta-icon">♡</i>{{ likes }} likes</span>

    <span class="art-meta-sep" />
    <span class="art-meta-item"><i class="art-meta-icon">★</i>{{ collects }} 收藏</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Article } from '@/types/article'
import { formatCount } from '@/utils/articleMapper'

const props = defineProps<{ article: Article }>()

const initial = computed(() => props.article.author.name.slice(0, 1).toUpperCase())
const views = computed(() => formatCount(props.article.viewCount))
const likes = computed(() => formatCount(props.article.likeCount))
const collects = computed(() => formatCount(props.article.collectCount))
</script>
