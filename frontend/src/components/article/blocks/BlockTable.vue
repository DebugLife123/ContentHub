<template>
  <div class="art-table-wrap">
    <table class="art-table">
      <thead>
        <tr>
          <th v-for="(head, index) in block.header" :key="index">{{ head }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, rowIndex) in block.rows" :key="rowIndex">
          <td v-for="(cell, cellIndex) in row" :key="cellIndex">
            <InlineText :segments="cellSegments(cell)" />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { TableBlock } from '@/types/article'
import { parseInline } from '@/utils/articleParser'
import InlineText from '../InlineText.vue'

defineProps<{ block: TableBlock }>()

/** 单元格支持行内代码 / 粗体 / 链接 */
function cellSegments(cell: string) {
  return parseInline(cell)
}
</script>
