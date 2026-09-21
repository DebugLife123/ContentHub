<template>
  <div class="art-formula">{{ block.formula }}</div>
</template>

<script setup lang="ts">
import type { FormulaBlock } from '@/types/article'

defineProps<{ block: FormulaBlock }>()
</script>
