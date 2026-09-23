<script setup lang="ts">
/**
 * 错误状态（带重试）。
 *
 * 改造前各页面 catch 到异常只弹一个 ElMessage 就结束了，
 * 没有重试入口 —— 网络抖一下用户只能自己刷新整个页面。
 * 请求失败是可恢复的，必须给一个「再试一次」。
 *
 * 用法：
 *   <AppError v-if="error" :message="error" :retrying="loading" @retry="load" />
 */
withDefaults(
  defineProps<{
    title?: string
    /** 具体错误信息，展示在标题下方 */
    message?: string
    /** 重试请求进行中：按钮进入 loading 且不可重复点击 */
    retrying?: boolean
  }>(),
  { title: '内容加载失败' },
)

const emit = defineEmits<{ (e: 'retry'): void }>()
</script>

<template>
  <div class="err" role="alert">
    <div class="err-mark" aria-hidden="true">!</div>
    <p class="err-title">{{ title }}</p>
    <p class="err-desc">
      {{ message || '网络可能不太稳定，或者是服务端暂时没能响应。' }}
    </p>

    <div class="err-actions">
      <button class="err-retry" type="button" :disabled="retrying" @click="emit('retry')">
        {{ retrying ? '正在重试…' : '再试一次' }}
      </button>
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.err {
  padding: 64px 0 72px;
  text-align: center;
}

.err-mark {
  width: 44px;
  height: 44px;
  margin: 0 auto 20px;
  border: 1px solid var(--line);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font: 500 18px var(--font-mono);
  color: #a33b2a;
}

.err-title {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.02em;
  margin: 0;
  color: var(--ink);
}

.err-desc {
  font-size: 13px;
  line-height: 1.8;
  color: var(--muted);
  margin: 10px auto 0;
  max-width: 360px;
}

.err-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 26px;
}

.err-retry {
  border: 0;
  background: var(--ink);
  color: #fff;
  cursor: pointer;
  padding: 12px 20px;
  font: 500 11px var(--font-mono);
  border-radius: 2px;
}

.err-retry:disabled {
  opacity: 0.5;
  cursor: default;
}

.err-retry:focus-visible {
  outline: 2px solid var(--orange);
  outline-offset: 2px;
}
</style>
