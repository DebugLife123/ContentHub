<template>
  <div class="app-shell">
    <!-- 懒加载路由首次进入需要下载 chunk，用它填补这段无反馈的空白 -->
    <RouteProgress />

    <!--
      管理后台是独立整屏控制台：由 AdminLayout 自己提供侧边栏与顶栏，
      因此这里不再套站点头部 / 页脚，避免出现"页面里还有一层网站外壳"。
    -->
    <template v-if="isAdminLayout">
      <router-view />
    </template>

    <template v-else>
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
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
import RouteProgress from './components/RouteProgress.vue'

const route = useRoute()

/** 路由 meta.layout === 'admin' 时走整屏控制台布局（子路由会继承该 meta） */
const isAdminLayout = computed(() => route.meta.layout === 'admin')
</script>
