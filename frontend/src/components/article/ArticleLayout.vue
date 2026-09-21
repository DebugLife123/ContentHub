<template>
  <div class="art-page">
    <div class="art-layout">
      <div class="art-toc-slot">
        <slot name="toc" />
      </div>
      <main class="art-main">
        <slot />
      </main>
      <div class="art-aside-slot">
        <slot name="aside" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 文章详情页三栏栅格：左 TOC / 中正文 / 右 Sidebar。
 * 断点行为见 docs/article-design-system.md 附录。
 */
</script>
