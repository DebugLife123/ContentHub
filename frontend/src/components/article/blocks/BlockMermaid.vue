<template>
  <!--
    Mermaid 降级展示：以代码块呈现图源码，并预留渲染引擎接入点。
    后续接入 mermaid.js 时，只需在此组件内替换渲染逻辑，数据结构不变。
  -->
  <BlockCode
    :block="{ type: 'code', language: 'mermaid', filename: 'diagram.mmd', code: block.code }"
    note="图表源码 · Mermaid 渲染引擎接入点已预留"
  />
</template>

<script setup lang="ts">
import type { MermaidBlock } from '@/types/article'
import BlockCode from './BlockCode.vue'

defineProps<{ block: MermaidBlock }>()
</script>
