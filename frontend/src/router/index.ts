import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import type { Role } from '@/api/types'

import Home from '../views/Home.vue'
import ContentList from '../views/ContentList.vue'
import ContentDetail from '../views/ContentDetail.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Profile from '../views/Profile.vue'
import Plans from '../views/subscription/Plans.vue'
import MySubscriptions from '../views/subscription/MySubscriptions.vue'
import CreatorDashboard from '../views/CreatorDashboard.vue'
import CreatorProfile from '../views/creator/Profile.vue'
import CreatorPlans from '../views/creator/Plans.vue'
import EditContent from '../views/creator/EditContent.vue'
import CategoryManage from '../views/admin/CategoryManage.vue'
import ContentReview from '../views/admin/ContentReview.vue'
import AdminUsers from '../views/admin/Users.vue'
import AdminComments from '../views/admin/Comments.vue'
import AdminPlans from '../views/admin/Plans.vue'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** 需要登录 */
    requiresAuth?: boolean
    /** 允许访问的角色，未设置表示登录即可 */
    roles?: Role[]
  }
}

const routes: RouteRecordRaw[] = [
  { path: '/', component: Home, meta: { title: '发现内容' } },
  { path: '/contents', component: ContentList, meta: { title: '内容库' } },
  { path: '/content/:id', component: ContentDetail, meta: { title: '内容详情' } },
  { path: '/login', component: Login, meta: { title: '登录' } },
  { path: '/register', component: Register, meta: { title: '注册' } },

  // ---------- 订阅方案（公开可看，购买需登录） ----------
  { path: '/plans', component: Plans, meta: { title: '订阅方案' } },

  // ---------- 需要登录 ----------
  { path: '/profile', component: Profile, meta: { title: '个人中心', requiresAuth: true } },
  {
    path: '/subscriptions',
    component: MySubscriptions,
    meta: { title: '我的订阅', requiresAuth: true },
  },

  // ---------- 创作者 ----------
  {
    path: '/creator',
    component: CreatorDashboard,
    meta: { title: '创作者工作台', requiresAuth: true, roles: ['CREATOR', 'ADMIN'] },
  },
  {
    path: '/creator/profile',
    component: CreatorProfile,
    meta: { title: '创作者资料', requiresAuth: true, roles: ['CREATOR', 'ADMIN'] },
  },
  {
    path: '/creator/plans',
    component: CreatorPlans,
    meta: { title: '订阅套餐管理', requiresAuth: true, roles: ['CREATOR', 'ADMIN'] },
  },
  {
    path: '/creator/contents/new',
    component: EditContent,
    meta: { title: '发布内容', requiresAuth: true, roles: ['CREATOR', 'ADMIN'] },
  },
  {
    path: '/creator/contents/:id/edit',
    component: EditContent,
    meta: { title: '编辑内容', requiresAuth: true, roles: ['CREATOR', 'ADMIN'] },
  },

  // ---------- 管理员（阶段 6：三种角色看到不同后台功能） ----------
  {
    path: '/admin/contents',
    component: ContentReview,
    meta: { title: '内容审核', requiresAuth: true, roles: ['ADMIN'] },
  },
  {
    path: '/admin/users',
    component: AdminUsers,
    meta: { title: '用户管理', requiresAuth: true, roles: ['ADMIN'] },
  },
  {
    path: '/admin/comments',
    component: AdminComments,
    meta: { title: '评论管理', requiresAuth: true, roles: ['ADMIN'] },
  },
  {
    path: '/admin/categories',
    component: CategoryManage,
    meta: { title: '分类管理', requiresAuth: true, roles: ['ADMIN'] },
  },
  {
    path: '/admin/plans',
    component: AdminPlans,
    meta: { title: '套餐管理', requiresAuth: true, roles: ['ADMIN'] },
  },

  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

/**
 * 路由守卫（计划 Day 18）。
 *
 * <p>这只是前端的体验层拦截，真正的权限由后端 Spring Security 决定。
 * 前端守卫可以被绕过（改 localStorage 即可），所以两边都必须做。</p>
 */
router.beforeEach(async (to) => {
  const userStore = useUserStore()

  // 刷新页面后 store 里只剩 token，要先补齐用户信息才能判断角色
  if (userStore.token && !userStore.userInfo) {
    await userStore.fetchCurrentUser()
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles && to.meta.roles.length > 0) {
    if (!userStore.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    if (!userStore.hasRole(...to.meta.roles)) {
      // 已登录但角色不够：回首页，避免出现「登录了却要我登录」的困惑
      return { path: '/' }
    }
  }

  return true
})

router.afterEach((to) => {
  document.title = `${to.meta.title || 'ContentHub'} · ContentHub`
})

export default router
