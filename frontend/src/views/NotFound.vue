<script setup lang="ts">
/**
 * 404 页面。
 *
 * 改造前未匹配的路由是 `redirect: '/'`，用户输错地址会莫名其妙回到首页。
 * 现在保留原始路径并给出明确的出口。
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

/** 原始请求路径（去掉开头的 /，空路径显示为「/」） */
const path = computed(() => route.fullPath)

function goBack() {
  // 直接打开 404 时没有上一页，回退会退出站点，因此退回首页更稳妥
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<template>
  <section class="nf">
    <p class="nf-eyebrow">ERROR / 404</p>
    <h1 class="nf-title">这个页面<em>不存在</em></h1>
    <p class="nf-desc">地址可能拼错了，或者这个内容已经被作者下架。</p>
    <p class="nf-path">请求路径 <code>{{ path }}</code></p>

    <div class="nf-actions">
      <router-link class="nf-primary" to="/">回到首页<span>→</span></router-link>
      <button class="nf-ghost" type="button" @click="goBack">返回上一页</button>
    </div>
  </section>
</template>

<style scoped>
.nf {
  width: min(var(--content-width), calc(100% - var(--page-gutter)));
  margin: 0 auto;
  padding: 140px 0 180px;
  max-width: 640px;
}

.nf-eyebrow {
  font: 11px var(--font-mono);
  letter-spacing: 0.08em;
  color: var(--muted);
  margin: 0 0 21px;
}

.nf-title {
  font-size: clamp(40px, 5vw, 64px);
  line-height: 1.06;
  letter-spacing: -0.07em;
  margin: 0;
  font-weight: 700;
}

.nf-title em {
  font-family: var(--font-serif);
  font-style: normal;
  font-weight: 600;
}

.nf-desc {
  font-size: 15px;
  line-height: 1.9;
  color: var(--muted);
  margin: 26px 0 0;
  max-width: 380px;
}

.nf-path {
  font-size: 13px;
  color: var(--muted);
  margin: 18px 0 0;
}

.nf-path code {
  font-family: var(--font-mono);
  font-size: 12px;
  background: var(--art-surface, #edeae2);
  padding: 3px 8px;
  border-radius: 4px;
  word-break: break-all;
}

.nf-actions {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-top: 46px;
}

.nf-primary {
  background: var(--ink);
  color: #fff;
  text-decoration: none;
  padding: 15px 20px;
  font: 500 12px var(--font-mono);
}

.nf-primary span {
  margin-left: 18px;
  color: var(--orange);
}

.nf-ghost {
  border: 0;
  background: none;
  padding: 0 0 4px;
  cursor: pointer;
  font: 11px var(--font-mono);
  color: var(--ink);
  border-bottom: 1px solid var(--ink);
}

@media (max-width: 800px) {
  .nf {
    padding: 90px 0 120px;
  }
}
</style>
