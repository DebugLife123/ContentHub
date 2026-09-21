<template>
  <figure class="art-figure">
    <img
      class="art-figure-img"
      :src="block.url"
      :alt="block.alt || block.caption || '文章插图'"
      loading="lazy"
      @click="zoom = true"
    >
    <figcaption v-if="block.caption" class="art-figure-caption">
      <span class="art-figure-no">图 {{ number }}</span>{{ block.caption }}
    </figcaption>

    <el-dialog
      v-model="zoom"
      class="art-lightbox"
      :show-close="true"
      width="min(1080px, 92vw)"
      append-to-body
    >
      <img :src="block.url" :alt="block.alt || ''">
    </el-dialog>
  </figure>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ImageBlock } from '@/types/article'

defineProps<{ block: ImageBlock; number: string }>()

const zoom = ref(false)
</script>
