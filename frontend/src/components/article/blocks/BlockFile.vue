<template>
  <div class="art-file">
    <span class="art-file-icon" aria-hidden="true">📄</span>
    <span class="art-file-name">{{ block.name }}</span>
    <a class="art-file-link" :href="block.url" target="_blank" rel="noopener">下载附件 ↗</a>
  </div>
</template>

<script setup lang="ts">
import type { FileBlock } from '@/types/article'

defineProps<{ block: FileBlock }>()
</script>
