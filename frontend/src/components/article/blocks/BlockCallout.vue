<template>
  <aside class="art-callout" :class="`art-callout--${block.variant}`" role="note">
    <span class="art-callout-icon" aria-hidden="true">{{ icon }}</span>
    <span class="art-callout-title">{{ block.title }}</span>
    <div class="art-callout-body">
      <InlineText :segments="block.segments" />
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CalloutBlock, CalloutVariant } from '@/types/article'
import InlineText from '../InlineText.vue'

const props = defineProps<{ block: CalloutBlock }>()

const ICONS: Record<CalloutVariant, string> = {
  tip: '💡',
  info: 'ℹ️',
  warning: '⚠️',
  danger: '⛔',
  success: '✅',
}

const icon = computed(() => ICONS[props.block.variant])
</script>
