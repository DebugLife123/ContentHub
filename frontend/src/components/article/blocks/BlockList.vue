<template>
  <ul
    class="art-list"
    :class="{ 'art-list--ordered': block.ordered, 'art-list--task': block.task }"
  >
    <li
      v-for="(item, index) in block.items"
      :key="index"
      class="art-list-item"
      :class="{ 'is-done': item.checked }"
    >
      <span
        v-if="block.task"
        class="art-task-box"
        :class="{ 'is-checked': item.checked }"
        aria-hidden="true"
      >{{ item.checked ? '✓' : '' }}</span>
      <InlineText :segments="item.segments" />
    </li>
  </ul>
</template>

<script setup lang="ts">
import type { ListBlock } from '@/types/article'
import InlineText from '../InlineText.vue'

defineProps<{ block: ListBlock }>()
</script>
