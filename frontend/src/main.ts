import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles/contenthub.css'
import './styles/article-system.scss'
import './styles/admin-system.scss'

const app = createApp(App)

const pinia = createPinia()
// 注册持久化插件，stores/user.ts 中的 persist: true 才会生效
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
