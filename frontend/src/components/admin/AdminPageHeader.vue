<template>
  <header class="admin-page-head">
    <div>
      <p class="admin-page-eyebrow">{{ eyebrow }}</p>
      <h1 class="admin-page-title">
        {{ title }}<template v-if="accent"> <em>{{ accent }}</em></template>
      </h1>
      <p v-if="description" class="admin-page-desc">{{ description }}</p>
    </div>

    <div v-if="$slots.actions" class="admin-page-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
/**
 * 管理端共享页头。
 * 尺寸按"控制台"收敛（27px），不再用前台的 54px 展示型标题，
 * 但保留 eyebrow + 衬线斜体点缀的品牌基因。
 */
withDefaults(defineProps<{
  eyebrow: string
  title: string
  accent?: string
  description?: string
}>(), {
  accent: '',
  description: '',
})
</script>
