<template>
  <figure class="art-code">
    <figcaption class="art-code-toolbar">
      <span class="art-code-dots" aria-hidden="true"><i /><i /><i /></span>
      <span class="art-code-filename">{{ block.filename || languageLabel }}</span>
      <span class="art-code-lang">{{ languageLabel }}</span>
      <button
        class="art-code-copy"
        :class="{ 'is-copied': copied }"
        type="button"
        @click="copy"
      >{{ copied ? '已复制' : '复制' }}</button>
    </figcaption>

    <div class="art-code-scroll">
      <div class="art-code-pre" role="presentation">
        <div v-for="(line, index) in lines" :key="index" class="art-code-line">
          <span class="art-code-gutter">{{ index + 1 }}</span>
          <span class="art-code-text">{{ line || ' ' }}</span>
        </div>
      </div>
    </div>

    <div v-if="note" class="art-mermaid-note">{{ note }}</div>
  </figure>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { CodeBlock } from '@/types/article'

const props = defineProps<{ block: CodeBlock; note?: string }>()

const copied = ref(false)

const lines = computed(() => props.block.code.split('\n'))

const languageLabel = computed(() => props.block.language.toUpperCase() || 'TEXT')

async function copy() {
  try {
    await navigator.clipboard.writeText(props.block.code)
  } catch {
    // 剪贴板 API 不可用（非安全上下文）时的降级方案
    const textarea = document.createElement('textarea')
    textarea.value = props.block.code
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
  }
  copied.value = true
  window.setTimeout(() => { copied.value = false }, 1800)
}
</script>
