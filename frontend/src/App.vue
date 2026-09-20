<template>
  <div class="app-shell">
    <AppHeader />
    <main class="app-main">
      <router-view />
    </main>
    <footer class="site-footer">
      <div class="footer-inner">
        <span class="footer-brand">CONTENTHUB / 01</span>
        <span>让知识拥有持续被发现的时间。</span>
        <span>© 2026 ContentHub</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import AppHeader from './components/AppHeader.vue'
</script>