import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig({
  server: {
    port: 5175,
    proxy: {
      // 后端已用 server.servlet.context-path=/api 统一加前缀，
      // 因此这里不剥掉 /api，原样转发。
      '/api': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
    },
  },
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    /*
     * Element Plus 按需引入（模板组件部分）。
     *
     * <el-xxx> 会在这里被自动引入，连同它自己的 theme-chalk 样式。
     * ElMessage / ElMessageBox 这类 JS 调用式 API 没有模板标签，resolver 管不到，
     * 它们的样式在 main.ts 里显式引入（合计约 46KB）。
     *
     * 注：常见做法是再装一个 unplugin-element-plus 自动处理，但它是纯 ESM 包，
     * Vite 4 的配置加载器（CJS）加载不了，因此改为显式引入，反而更直观。
     */
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  build: {
    /*
     * 分包策略。
     *
     * 改造前所有路由都是静态 import，产物是单个 1.09MB 的 JS。
     * 现在路由已懒加载，这里只需要把「内容稳定、不随业务变化」的框架依赖
     * 单独切出来长期缓存即可 —— 业务代码更新时浏览器不必重新下载它们。
     *
     * 注意：不要用 `{ 'element-plus': ['element-plus'] }` 这种写法。
     * 那会把整个组件库强制打进同一块，而外壳（AppHeader）本身就用了
     * Element Plus，于是首屏会立刻把这 339KB 全量拉下来，
     * 管理后台专用的组件也一并加载，懒加载就白做了。
     * 交给 Rollup 默认算法按「谁真正引用」来切，才会有正确的按需效果。
     */
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          if (!id.includes('node_modules')) return undefined
          if (/[\\/]node_modules[\\/](vue|vue-router|pinia|@vue)[\\/]/.test(id)) return 'vue'
          return undefined
        },
      },
    },
    // 单个 chunk 超过 600KB 才告警
    chunkSizeWarningLimit: 600,
  },
  resolve: {
    alias: {
      // 定义一个别名 '@'，该别名对应于当前模块文件所在目录下的 'src' 目录的绝对文件路径。
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
