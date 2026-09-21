<template>
  <div class="coming-page content-width">
    <p class="eyebrow">{{ meta.eyebrow || 'COMING SOON' }}</p>
    <h1>{{ title }}<br /><em>正在建设中</em></h1>
    <p class="coming-lede">{{ meta.description }}</p>

    <div class="coming-grid">
      <div v-for="(item, index) in meta.highlights || []" :key="item" class="coming-card">
        <span class="coming-index">{{ String(index + 1).padStart(2, '0') }}</span>
        <strong>{{ item }}</strong>
      </div>
    </div>

    <div class="coming-actions">
      <RouterLink class="button button-dark" to="/contents">先去内容库看看 <span>↗</span></RouterLink>
      <span class="coming-note">该模块为占位页，功能尚未实现。</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const meta = computed(() => route.meta)
const title = computed(() => (route.meta.title as string) || '新功能')
</script>

<style scoped>
.coming-page {
  padding: 110px 0 60px;
  min-height: 62vh;
}
.coming-page h1 {
  font-size: clamp(42px, 5.6vw, 76px);
  line-height: 1.05;
  letter-spacing: -0.07em;
  font-weight: 700;
  margin: 0;
}
.coming-page h1 em {
  font-family: 'Noto Serif SC', serif;
  font-style: normal;
  font-weight: 600;
}
.coming-lede {
  max-width: 460px;
  margin: 30px 0 0;
  font-size: 15px;
  line-height: 1.9;
  color: var(--muted);
}
.coming-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1px;
  background: var(--line);
  margin: 55px 0 0;
  border: 1px solid var(--line);
}
.coming-card {
  background: var(--paper);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.coming-index {
  font: 10px 'DM Mono', monospace;
  color: var(--orange);
}
.coming-card strong {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.02em;
}
.coming-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 45px;
}
.coming-note {
  font: 10px 'DM Mono', monospace;
  color: var(--muted);
}
</style>
