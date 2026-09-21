<template>
  <figure class="art-code">
    <figcaption class="art-code-toolbar">
      <span class="art-api-method" :class="methodClass">{{ block.method }}</span>
      <span class="art-api-path">{{ block.path }}</span>
      <span class="art-code-lang">{{ block.language.toUpperCase() }}</span>
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
  </figure>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ApiBlock } from '@/types/article'

const props = defineProps<{ block: ApiBlock }>()

const copied = ref(false)

const lines = computed(() => props.block.code.split('\n'))

const methodClass = computed(() => {
  const method = props.block.method.toLowerCase()
  return `art-api-method--${['get', 'post', 'put', 'delete', 'patch'].includes(method) ? method : 'get'}`
})

async function copy() {
  try {
    await navigator.clipboard.writeText(props.block.code)
  } catch {
    /* 忽略剪贴板失败 */
  }
  copied.value = true
  window.setTimeout(() => { copied.value = false }, 1800)
}
</script>
