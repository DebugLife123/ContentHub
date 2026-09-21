<template>
  <figure class="art-video">
    <video v-if="isFileVideo" :src="block.url" controls preload="metadata" />
    <iframe
      v-else
      :src="block.url"
      allowfullscreen
      loading="lazy"
      referrerpolicy="no-referrer"
    />
    <figcaption v-if="block.caption" class="art-figure-caption">{{ block.caption }}</figcaption>
  </figure>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { VideoBlock } from '@/types/article'

const props = defineProps<{ block: VideoBlock }>()

/** 直链视频文件用 <video>，否则按嵌入页（YouTube / Bilibili iframe）处理 */
const isFileVideo = computed(() => /\.(mp4|webm|mov|ogg)(\?|$)/i.test(props.block.url))
</script>
