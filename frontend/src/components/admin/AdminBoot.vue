<template>
  <Transition name="admin-boot" appear>
    <div v-if="visible" class="admin-boot">
      <div class="admin-boot-inner">
        <div class="admin-boot-mark">
          <span class="brand-dot" />
          <span>Content<span>Hub</span></span>
        </div>
        <p class="admin-boot-sub">ADMIN CONSOLE</p>
        <div class="admin-boot-bar"><i /></div>
        <p class="admin-boot-hint">{{ hint }}</p>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 进入管理后台的加载过场。
 *
 * 刻意做成「短、静、有终点」的一段：一条 820ms 的进度线 + 三句状态文案，
 * 结束后与内容交叉淡入。不做循环动画或假进度，避免拖慢真实操作。
 * 开启「减少动态效果」时直接跳过。
 */
const emit = defineEmits<{ (e: 'done'): void }>()

const HINTS = ['正在校验管理员权限…', '装载管理模块…', '就绪']
const DURATION = 820

const visible = ref(true)
const hint = ref(HINTS[0])
const timers: number[] = []

onMounted(() => {
  if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
    visible.value = false
    emit('done')
    return
  }

  timers.push(
    window.setTimeout(() => { hint.value = HINTS[1] }, 270),
    window.setTimeout(() => { hint.value = HINTS[2] }, 610),
    // 遮罩开始淡出时同步通知父级把内容淡入，形成交叉过渡
    window.setTimeout(() => {
      visible.value = false
      emit('done')
    }, DURATION),
  )
})

onBeforeUnmount(() => timers.forEach(clearTimeout))
</script>
