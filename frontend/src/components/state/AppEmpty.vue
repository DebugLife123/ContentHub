<script setup lang="ts">
/**
 * 空状态。
 *
 * 关键在于区分两种「没有数据」：
 *   - 一条都没有        → 说明这个模块还是空的，引导去创建
 *   - 筛选/搜索后没结果 → 说明条件太窄，引导去放宽条件
 * 两者文案和出口完全不同，混用会让用户以为是系统坏了。
 *
 * 用法：
 *   <AppEmpty v-if="!list.length" :filtered="hasFilter" @reset="resetFilter" />
 */
withDefaults(
  defineProps<{
    /** true = 筛选/搜索后无结果；false = 完全没有数据 */
    filtered?: boolean
    /** 覆盖默认标题 */
    title?: string
    /** 覆盖默认描述 */
    description?: string
  }>(),
  { filtered: false },
)

const emit = defineEmits<{ (e: 'reset'): void }>()
</script>

<template>
  <div class="empty">
    <div class="empty-mark" aria-hidden="true">{{ filtered ? '⌕' : '∅' }}</div>
    <p class="empty-title">{{ title || (filtered ? '没有匹配的结果' : '这里还没有内容') }}</p>
    <p class="empty-desc">
      {{ description || (filtered ? '换一个关键词，或者放宽筛选条件再试试。' : '等第一条内容出现后，它会显示在这里。') }}
    </p>

    <div v-if="$slots.actions || filtered" class="empty-actions">
      <slot name="actions">
        <button class="empty-reset" type="button" @click="emit('reset')">清空筛选条件</button>
      </slot>
    </div>
  </div>
</template>

<style scoped>
.empty {
  padding: 72px 0 80px;
  text-align: center;
}

.empty-mark {
  font-family: var(--font-mono);
  font-size: 34px;
  line-height: 1;
  color: var(--line);
  margin-bottom: 20px;
}

.empty-title {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.02em;
  margin: 0;
  color: var(--ink);
}

.empty-desc {
  font-size: 13px;
  line-height: 1.8;
  color: var(--muted);
  margin: 10px auto 0;
  max-width: 320px;
}

.empty-actions {
  margin-top: 26px;
}

.empty-reset {
  border: 1px solid var(--line);
  background: none;
  cursor: pointer;
  padding: 10px 16px;
  font: 11px var(--font-mono);
  color: var(--ink);
  border-radius: 2px;
  transition: border-color var(--dur-fast) var(--ease-out);
}

.empty-reset:hover {
  border-color: var(--ink);
}

.empty-reset:focus-visible {
  outline: 2px solid var(--orange);
  outline-offset: 2px;
}
</style>
