<template>
  <template v-for="(segment, index) in segments" :key="index">
    <strong v-if="segment.kind === 'bold'" class="art-strong">{{ segment.text }}</strong>
    <code v-else-if="segment.kind === 'code'" class="art-inline-code">{{ segment.text }}</code>
    <a
      v-else-if="segment.kind === 'link'"
      class="art-link"
      :href="linkHref(segment)"
      target="_blank"
      rel="noopener"
    >{{ segment.text }}</a>
    <template v-else>{{ segment.text }}</template>
  </template>
</template>

<script setup lang="ts">
import type { InlineSegment } from '@/types/article'

defineProps<{ segments: InlineSegment[] }>()

function linkHref(segment: InlineSegment): string {
  return segment.kind === 'link' ? segment.href : '#'
}
</script>
