<template>
  <div class="art-body" :class="{ 'art-preview-mask': preview }">
    <template v-for="(block, index) in blocks" :key="index">
      <!-- H2：杂志式章节（编号 + 标题 + 分隔线） -->
      <section v-if="block.type === 'heading' && block.level === 2" class="art-section">
        <div v-if="block.sectionNo" class="art-section-no">{{ block.sectionNo }}</div>
        <h2 :id="block.id" class="art-h2">{{ block.text }}</h2>
      </section>

      <h3
        v-else-if="block.type === 'heading' && block.level === 3"
        :id="block.id"
        class="art-h3"
      >{{ block.text }}</h3>

      <h4
        v-else-if="block.type === 'heading'"
        :id="block.id"
        class="art-h4"
      >{{ block.text }}</h4>

      <p v-else-if="block.type === 'paragraph'" class="art-p">
        <InlineText :segments="block.segments" />
      </p>

      <BlockQuote v-else-if="block.type === 'quote'" :block="block" />

      <BlockCode v-else-if="block.type === 'code'" :block="block" />

      <BlockCallout v-else-if="block.type === 'callout'" :block="block" />

      <BlockImage
        v-else-if="block.type === 'image'"
        :block="block"
        :number="imageNumber(index)"
      />

      <BlockTable v-else-if="block.type === 'table'" :block="block" />

      <BlockList v-else-if="block.type === 'list'" :block="block" />

      <BlockDivider v-else-if="block.type === 'divider'" />

      <BlockVideo v-else-if="block.type === 'video'" :block="block" />

      <BlockFile v-else-if="block.type === 'file'" :block="block" />

      <BlockRepo v-else-if="block.type === 'repo'" :block="block" />

      <BlockApi v-else-if="block.type === 'api'" :block="block" />

      <BlockFormula v-else-if="block.type === 'formula'" :block="block" />

      <BlockMermaid v-else-if="block.type === 'mermaid'" :block="block" />
    </template>
  </div>
</template>

<script setup lang="ts">
import type { ArticleBlock } from '@/types/article'
import InlineText from './InlineText.vue'
import BlockApi from './blocks/BlockApi.vue'
import BlockCallout from './blocks/BlockCallout.vue'
import BlockCode from './blocks/BlockCode.vue'
import BlockDivider from './blocks/BlockDivider.vue'
import BlockFile from './blocks/BlockFile.vue'
import BlockFormula from './blocks/BlockFormula.vue'
import BlockImage from './blocks/BlockImage.vue'
import BlockList from './blocks/BlockList.vue'
import BlockMermaid from './blocks/BlockMermaid.vue'
import BlockQuote from './blocks/BlockQuote.vue'
import BlockRepo from './blocks/BlockRepo.vue'
import BlockTable from './blocks/BlockTable.vue'
import BlockVideo from './blocks/BlockVideo.vue'

const props = defineProps<{
  blocks: ArticleBlock[]
  /** 试读模式：底部渐隐遮罩 */
  preview?: boolean
}>()

/** 图片自动编号：图 01 / 图 02 … */
function imageNumber(index: number): string {
  const count = props.blocks
    .slice(0, index)
    .filter((block) => block.type === 'image')
    .length
  return String(count + 1).padStart(2, '0')
}
</script>
