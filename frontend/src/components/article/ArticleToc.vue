<template>
  <nav v-if="toc.length" aria-label="文章目录">
    <p class="art-toc-title">文章目录</p>
    <ul class="art-toc">
      <li v-for="item in toc" :key="item.id" class="art-toc-item">
        <a
          class="art-toc-link"
          :class="{
            'is-active': activeId === item.id,
            'art-toc-link--h3': item.level === 3,
          }"
          :href="`#${item.id}`"
          @click.prevent="scrollTo(item.id)"
        >
          <span v-if="item.sectionNo" class="art-toc-no">{{ item.sectionNo }}</span>
          <span>{{ item.text }}</span>
        </a>
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import type { TocItem } from '@/types/article'

const props = defineProps<{ toc: TocItem[] }>()

const activeId = ref('')

let observer: IntersectionObserver | null = null

/**
 * scroll-spy：观察正文中的 H2/H3 锚点。
 * 注意：项目路由是 hash 模式，不能直接跳 #anchor，
 * 必须 scrollIntoView，否则会改写路由。
 */
function setupObserver() {
  observer?.disconnect()
  observer = null
  if (!props.toc.length || typeof IntersectionObserver === 'undefined') return

  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) activeId.value = entry.target.id
      })
    },
    // 顶部 104px 是 sticky topbar；把“当前章节”判定线放在视口上部
    { rootMargin: '-104px 0px -68% 0px', threshold: 0 },
  )

  props.toc.forEach((item) => {
    const el = document.getElementById(item.id)
    if (el) observer?.observe(el)
  })

  // 默认高亮第一个
  if (!activeId.value) activeId.value = props.toc[0].id
}

function scrollTo(id: string) {
  activeId.value = id
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 正文是异步渲染的，toc 变化后等 DOM 就绪再观察
watch(() => props.toc, async () => {
  await nextTick()
  setupObserver()
}, { immediate: true, deep: true })

onBeforeUnmount(() => observer?.disconnect())
</script>
