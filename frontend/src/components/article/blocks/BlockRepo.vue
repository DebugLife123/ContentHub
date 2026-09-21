<template>
  <div class="art-repo">
    <span class="art-repo-icon" aria-hidden="true">▣</span>
    <a class="art-repo-name" :href="repoUrl" target="_blank" rel="noopener">{{ block.repo }}</a>
    <span v-if="block.description" class="art-repo-desc">{{ block.description }}</span>
    <a class="art-file-link" :href="repoUrl" target="_blank" rel="noopener">GitHub ↗</a>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RepoBlock } from '@/types/article'

const props = defineProps<{ block: RepoBlock }>()

const repoUrl = computed(() => `https://github.com/${props.block.repo}`)
</script>
