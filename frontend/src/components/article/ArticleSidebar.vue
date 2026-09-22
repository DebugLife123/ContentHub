<template>
  <aside>
    <!-- 作者 -->
    <div class="art-aside-block">
      <p class="art-aside-title">作者</p>
      <component
        :is="article.author.id ? 'RouterLink' : 'div'"
        :to="article.author.id ? `/creators/${article.author.id}` : undefined"
        class="art-author"
        :class="{ 'is-link': !!article.author.id }"
      >
        <span class="art-avatar">
          <img v-if="article.author.avatar" :src="article.author.avatar" :alt="article.author.name">
          <template v-else>{{ initial }}</template>
        </span>
        <div>
          <span class="art-author-name">{{ article.author.name }}</span>
          <span class="art-author-bio">
            {{ article.author.bio || 'ContentHub 数字内容创作者' }}
            <template v-if="article.author.id"> · 查看主页 →</template>
          </span>
        </div>
      </component>
    </div>

    <!-- 文章数据 -->
    <div class="art-aside-block">
      <p class="art-aside-title">文章数据</p>
      <div class="art-stats">
        <div class="art-stat">
          <strong>{{ views }}</strong>
          <span>阅读</span>
        </div>
        <div class="art-stat">
          <strong>{{ likes }}</strong>
          <span>点赞</span>
        </div>
        <div class="art-stat">
          <strong>{{ collects }}</strong>
          <span>收藏</span>
        </div>
      </div>
    </div>

    <!-- 标签 -->
    <div v-if="article.tags.length" class="art-aside-block">
      <p class="art-aside-title">文章标签</p>
      <div class="art-tags">
        <RouterLink
          v-for="tag in article.tags"
          :key="tag"
          class="art-tag"
          :to="{ path: '/contents', query: { keyword: tag } }"
        ># {{ tag }}</RouterLink>
      </div>
    </div>

    <!-- 相关阅读 -->
    <div v-if="related.length" class="art-aside-block">
      <p class="art-aside-title">相关阅读</p>
      <ul class="art-related">
        <li v-for="item in related" :key="item.id" class="art-related-item">
          <RouterLink class="art-related-link" :to="`/content/${item.id}`">
            <strong>{{ item.title }}</strong>
            <span>{{ item.contentType || '内容' }} · {{ formatCount(item.viewCount) }} views</span>
          </RouterLink>
        </li>
      </ul>
    </div>

    <!-- 订阅 CTA：仅订阅专属内容展示 -->
    <div v-if="article.accessType !== 'FREE'" class="art-aside-block">
      <div class="art-cta">
        <strong>订阅创作者，解锁全部内容</strong>
        <p>订阅后可阅读该创作者的全部付费内容，并获取附件与更新推送。</p>
        <RouterLink class="art-cta-button" to="/plans">查看订阅方案 ↗</RouterLink>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Article, ArticleNavItem } from '@/types/article'
import { formatCount } from '@/utils/articleMapper'

const props = defineProps<{
  article: Article
  related: ArticleNavItem[]
}>()

const initial = computed(() => props.article.author.name.slice(0, 1).toUpperCase())
const views = computed(() => formatCount(props.article.viewCount))
const likes = computed(() => formatCount(props.article.likeCount))
const collects = computed(() => formatCount(props.article.collectCount))
</script>
