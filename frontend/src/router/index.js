import { createRouter, createWebHashHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import CreatorDashboard from '../views/CreatorDashboard.vue'
import ContentDetail from '../views/ContentDetail.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', component: Home, meta: { title: '发现内容' } },
    { path: '/login', component: Login, meta: { title: '登录' } },
    { path: '/creator', component: CreatorDashboard, meta: { title: '创作者工作台' } },
    { path: '/content/:id', component: ContentDetail, meta: { title: '内容详情' } },
  ],
})

router.afterEach((to) => {
  document.title = `${to.meta.title || 'ContentHub'} · ContentHub`
})

export default router