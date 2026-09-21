<template><div class="detail-page content-width"><RouterLink to="/" class="back-link">← 返回内容库</RouterLink><div v-if="loading" class="empty-state">正在加载内容…</div><div v-else-if="error" class="empty-state">{{ error }}</div><div v-else class="detail-layout"><article><p class="eyebrow">{{ contentType }} · CONTENT</p><h1>{{ content.title }}</h1><p class="detail-lede">{{ content.summary }}</p><div class="article-body"><p v-for="(paragraph, index) in paragraphs" :key="index">{{ paragraph }}</p></div></article><aside class="detail-aside"><div class="aside-cover theme-orange">CONTENT<br><strong>NOTE</strong><br><small>#{{ content.id }}</small></div><div class="author-box"><span class="avatar">C</span><div><strong>ContentHub Creator</strong><small>数字内容创作者</small></div></div><el-button class="button button-dark full-button">{{ content.accessType === 'FREE' ? '立即阅读' : '订阅后阅读全文' }} <span>↗</span></el-button><p class="aside-note">{{ content.accessType === 'FREE' ? '这份内容当前开放免费阅读。' : '加入作者的订阅计划，解锁完整内容。' }}</p></aside></div></div></template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getContent } from '../api/content'
import type { ContentItem } from '../api/types'

const route = useRoute()
const loading = ref(true)
const error = ref('')

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
const contentType = computed(() => content.value.contentType || 'FIELD NOTE')
const paragraphs = computed(() => (content.value.body || content.value.summary || '').split(/\n+/).filter(Boolean))

onMounted(async () => {
  try {
    const response = await getContent(String(route.params.id))
    if (!response.data.success) throw new Error(response.data.message || '内容不存在')
    content.value = response.data.data
  } catch (e) {
    error.value = e instanceof Error ? e.message : '内容加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
})
</script>
