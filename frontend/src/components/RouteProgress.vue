<script setup lang="ts">
/**
 * 路由级加载进度条。
 *
 * 页面改成懒加载后，首次进入某个路由需要先下载对应的 chunk，
 * 这段时间里界面是完全没反应的 —— 没有反馈的等待会让人觉得是卡死了。
 * 这条 2px 的进度条就是用来消除这段空白的。
 *
 * 实现要点：
 *   - 进度不会真的到 100%，而是缓慢逼近 90%，导航完成后才补满并淡出，
 *     避免出现「卡在 99% 不动」的观感；
 *   - 只在顶部占 2px，不遮挡内容、不改变布局。
 */
import { onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const visible = ref(false)
const progress = ref(0)
const finishing = ref(false)

let ticker: ReturnType<typeof setInterval> | undefined
let hideTimer: ReturnType<typeof setTimeout> | undefined

function clearTimers() {
  if (ticker) {
    clearInterval(ticker)
    ticker = undefined
  }
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = undefined
  }
}

function start() {
  clearTimers()
  finishing.value = false
  progress.value = 8
  visible.value = true
  ticker = setInterval(() => {
    const remaining = 90 - progress.value
    if (remaining > 0.5) {
      progress.value += remaining * 0.12
    }
  }, 140)
}

function finish() {
  if (!visible.value) return
  clearTimers()
  finishing.value = true
  progress.value = 100
  hideTimer = setTimeout(() => {
    visible.value = false
    progress.value = 0
    finishing.value = false
  }, 240)
}

// vue-router 4 的钩子注册函数会返回一个「移除该钩子」的函数
const removeBefore = router.beforeEach(() => {
  start()
  return true
})
const removeAfter = router.afterEach(() => finish())
const removeError = router.onError(() => finish())

onUnmounted(() => {
  removeBefore()
  removeAfter()
  removeError()
  clearTimers()
})
</script>

<template>
  <div
    v-show="visible"
    class="route-progress"
    :class="{ 'is-finishing': finishing }"
    :style="{ transform: `scaleX(${progress / 100})` }"
    role="progressbar"
    aria-label="页面加载中"
    :aria-valuenow="Math.round(progress)"
    aria-valuemin="0"
    aria-valuemax="100"
  />
</template>

<style scoped>
.route-progress {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--orange);
  transform-origin: 0 50%;
  transform: scaleX(0);
  transition: transform 0.18s var(--ease-out), opacity 0.24s linear;
  opacity: 1;
  z-index: 9999;
  pointer-events: none;
}

.route-progress.is-finishing {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .route-progress {
    transition: none;
  }
}
</style>
