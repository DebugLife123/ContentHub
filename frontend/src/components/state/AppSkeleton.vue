<script setup lang="ts">
/**
 * 骨架屏。
 *
 * 列表/详情在数据回来之前用它占位，而不是转圈 spinner ——
 * 骨架屏能提前给出页面结构，感知等待时间明显更短；
 * 转圈只告诉用户「在等」，还会让人怀疑是不是卡死了。
 *
 * 用法：
 *   <AppSkeleton v-if="loading" variant="cards" :count="6" />
 *   <AppSkeleton v-else-if="!list.length" /> 等等
 */
withDefaults(
  defineProps<{
    /** cards=卡片网格 / list=表格行 / article=长文段落 / text=单行 */
    variant?: 'cards' | 'list' | 'article' | 'text'
    /** 重复的条目数（text 变体忽略） */
    count?: number
  }>(),
  { variant: 'list', count: 5 },
)
</script>

<template>
  <!-- text：单行占位 -->
  <div v-if="variant === 'text'" class="sk sk-text" aria-hidden="true">
    <span class="sk-bar" />
  </div>

  <!-- article：长文段落占位 -->
  <div v-else-if="variant === 'article'" class="sk sk-article" aria-hidden="true">
    <span class="sk-bar sk-title" />
    <span v-for="i in 5" :key="i" class="sk-bar" :style="{ width: i === 5 ? '62%' : '100%' }" />
  </div>

  <!-- cards：卡片网格占位 -->
  <div v-else-if="variant === 'cards'" class="sk sk-cards" aria-hidden="true">
    <div v-for="i in count" :key="i" class="sk-card">
      <span class="sk-block" />
      <span class="sk-bar" style="width: 82%" />
      <span class="sk-bar" style="width: 54%" />
    </div>
  </div>

  <!-- list：表格/列表行占位（默认） -->
  <div v-else class="sk sk-list" aria-hidden="true">
    <div v-for="i in count" :key="i" class="sk-row">
      <span class="sk-bar" :style="{ width: 34 - (i % 3) * 5 + '%' }" />
      <span class="sk-bar sk-bar-sm" />
      <span class="sk-bar sk-bar-xs" />
    </div>
  </div>
</template>

<style scoped>
.sk {
  width: 100%;
}

/* 用一段缓慢移动的高光表示「正在加载」，比闪烁的方块安静 */
.sk-bar,
.sk-block {
  display: block;
  background: linear-gradient(90deg, #e8e4db 25%, #f2efe8 37%, #e8e4db 63%);
  background-size: 400% 100%;
  animation: sk-shimmer 1.4s ease-in-out infinite;
  border-radius: 3px;
}

@keyframes sk-shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

/* ---------------------------------------------------------------- text */
.sk-text .sk-bar {
  height: 16px;
}

/* ------------------------------------------------------------- article */
.sk-article .sk-bar {
  height: 15px;
  margin-bottom: 16px;
}

.sk-article .sk-title {
  height: 34px;
  width: 70%;
  margin-bottom: 28px;
}

/* --------------------------------------------------------------- cards */
.sk-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 24px;
}

.sk-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sk-card .sk-block {
  height: 168px;
  border-radius: 4px;
}

/* ---------------------------------------------------------------- list */
.sk-list {
  display: flex;
  flex-direction: column;
}

.sk-row {
  display: grid;
  grid-template-columns: 1fr 96px 72px;
  gap: 24px;
  align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid var(--line);
}

.sk-row .sk-bar {
  height: 13px;
}

.sk-bar-sm {
  height: 13px;
}

.sk-bar-xs {
  height: 13px;
}

/* 尊重系统的「减弱动效」设置 */
@media (prefers-reduced-motion: reduce) {
  .sk-bar,
  .sk-block {
    animation: none;
  }
}
</style>
