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
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      // 定义一个别名 '@'，该别名对应于当前模块文件所在目录下的 'src' 目录的绝对文件路径。
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
