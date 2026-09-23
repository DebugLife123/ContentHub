import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'

/*
 * 字体：全部自托管。
 *
 * 以前这里是 @import url('https://fonts.googleapis.com/css2?...')，
 * 但 fonts.googleapis.com 在国内不可达，线上实际回落到系统默认字体，
 * 设计规范里那套 Manrope / DM Mono 排版等于没生效。
 * 改成 @fontsource 打包进产物后，字体随静态资源一起发布，零外部依赖。
 *
 * 中文字体不打包：Noto Serif SC 完整包有 87MB，为 CJK 付这个下载成本不划算，
 * 统一走 tokens.css 里定义的系统字体回退栈。
 */
import '@fontsource/manrope/400.css'
import '@fontsource/manrope/500.css'
import '@fontsource/manrope/600.css'
import '@fontsource/manrope/700.css'
import '@fontsource/manrope/800.css'
import '@fontsource/dm-mono/400.css'
import '@fontsource/dm-mono/500.css'

/*
 * Element Plus：只引「函数式 API」需要的样式。
 *
 * 模板组件（<el-xxx>）的样式由 vite.config.ts 里的 ElementPlusResolver 按需引入；
 * 但 ElMessage / ElMessageBox 是 JS 调用出来的，没有模板标签，必须显式引。
 * 这 7 个文件合计约 46KB，而全量 element-plus/dist/index.css 是 318KB。
 *
 * 改造前 main.ts 里是 `app.use(ElementPlus)` + 全量 CSS，
 * 后者会把按需引入完全废掉。
 */
import 'element-plus/theme-chalk/base.css'
import 'element-plus/theme-chalk/el-icon.css'
import 'element-plus/theme-chalk/el-button.css'
import 'element-plus/theme-chalk/el-input.css'
import 'element-plus/theme-chalk/el-overlay.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'

/*
 * 全局样式。顺序就是层叠优先级，调整前先想清楚：
 *   tokens     设计变量（唯一事实来源）
 *   base       reset + 排版基线 + 动效工具类
 *   layout     站点外壳（顶栏 / 页脚）
 *   home       首页
 *   pages      登录注册 / 工作台 / 旧版详情页
 *   responsive 移动端覆盖，必须最后
 *   element-override  Element Plus 主题
 *   article / admin   两块各自独立的子系统样式
 */
import './styles/tokens.css'
import './styles/base.css'
import './styles/layout.css'
import './styles/home.css'
import './styles/pages.css'
import './styles/responsive.css'
import './styles/element-override.css'
import './styles/article-system.scss'
import './styles/admin-system.scss'

const app = createApp(App)

const pinia = createPinia()
// 注册持久化插件，stores/user.ts 中的 persist: true 才会生效
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)

/*
 * 这里刻意不做 app.use(ElementPlus)：
 * 全量注册会绕开上面的按需引入，把整个组件库打进产物。
 * 项目里没有使用 v-loading 指令或 $message 这类全局属性（都走显式 import），
 * 移除全量注册不会丢功能。
 */
app.mount('#app')
