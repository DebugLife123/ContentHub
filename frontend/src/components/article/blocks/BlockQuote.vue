<template>
  <blockquote class="art-quote">
    <InlineText :segments="block.segments" />
  </blockquote>
</template>

<script setup lang="ts">
import type { QuoteBlock } from '@/types/article'
import InlineText from '../InlineText.vue'

defineProps<{ block: QuoteBlock }>()
</script>
