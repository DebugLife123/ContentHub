import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import type { Role } from '@/api/types'

/*
 * 所有页面都改成动态 import（路由级懒加载）。
 *
 * 改造前这里是 27 个静态 import，Vite 会把所有页面打进同一个 chunk：
 * 游客打开首页也要先下载整个管理后台的代码（实测单文件 1.09MB）。
 * 改成动态 import 后每个页面独立成块，只有真正访问时才下载。
 *
 * App.vue / AppHeader.vue / AdminLayout.vue 这类「外壳」仍可静态引入，
 * 但它们同样被路由引用，一并懒加载即可。
 */

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** 需要登录 */
    requiresAuth?: boolean
    /** 允许访问的角色，未设置表示登录即可 */
    roles?: Role[]
    /** 整屏布局：'admin' 时交给管理后台外壳接管，不套站点头部与页脚 */
    layout?: 'admin'
  }
}

const routes: RouteRecordRaw[] = [
  { path: '/', component: () => import('../views/Home.vue'), meta: { title: '发现内容' } },
  { path: '/contents', component: () => import('../views/ContentList.vue'), meta: { title: '内容库' } },
  { path: '/content/:id', component: () => import('../views/ContentDetail.vue'), meta: { title: '内容详情' } },
  // 创作者公开主页（后端已有 GET /creators/{userId}，以前没有页面用它）
  { path: '/creators/:userId', component: () => import('../views/CreatorPublic.vue'), meta: { title: '创作者主页' } },
  { path: '/login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
  { path: '/register', component: () => import('../views/Register.vue'), meta: { title: '注册' } },

  // ---------- 订阅方案（公开可看，购买需登录） ----------
  { path: '/plans', component: () => import('../views/subscription/Plans.vue'), meta: { title: '订阅方案' } },

  // ---------- Skill 商城 ----------
  { path: '/skills', component: () => import('../views/skill/SkillList.vue'), meta: { title: 'Skill 商城' } },
  { path: '/skills/:id', component: () => import('../views/skill/SkillDetail.vue'), meta: { title: 'Skill 详情' } },

  // ---------- 需要登录 ----------
  {
    path: '/profile',
    component: () => import('../views/Profile.vue'),
    meta: { title: '个人中心', requiresAuth: true },
  },
  {
    path: '/notifications',
    component: () => import('../views/Notifications.vue'),
    meta: { title: '站内通知', requiresAuth: true },
  },
  {
    path: '/subscriptions',
    component: () => import('../views/subscription/MySubscriptions.vue'),
    meta: { title: '我的订阅', requiresAuth: true },
  },

  // ---------- 创作者 ----------
  {
    path: '/creator',
    component: () => import('../views/CreatorDashboard.vue'),
    meta: { title: '创作者工作台', requiresAuth: true, roles: ['CREATOR', 'ADMIN'] },
  },
  {
    path: '/creator/profile',
    component: () => import('../views/creator/Profile.vue'),
    meta: { title: '创作者资料', requiresAuth: true, roles: ['CREATOR', 'ADMIN'] },
  },
  {
    path: '/creator/plans',
    component: () => import('../views/creator/Plans.vue'),
    meta: { title: '订阅套餐管理', requiresAuth: true, roles: ['CREATOR', 'ADMIN'] },
  },
  {
    path: '/creator/revenue',
    component: () => import('../views/creator/Revenue.vue'),
    meta: { title: '创作者收益', requiresAuth: true, roles: ['CREATOR', 'ADMIN'] },
  },
  {
    path: '/creator/contents/new',
    component: () => import('../views/creator/EditContent.vue'),
    meta: { title: '发布内容', requiresAuth: true, roles: ['CREATOR', 'ADMIN'] },
  },
  {
    path: '/creator/contents/:id/edit',
    component: () => import('../views/creator/EditContent.vue'),
    meta: { title: '编辑内容', requiresAuth: true, roles: ['CREATOR', 'ADMIN'] },
  },

  // ---------- 管理后台（阶段 6 / 阶段 8：独立整屏控制台） ----------
  // 外层 AdminLayout 提供墨色侧边栏 + 顶栏的整屏外壳，子路由只换内容区；
  // 因此这里路径与以前完全一致（/admin/contents 等），已有链接无需改动。
  {
    path: '/admin',
    component: () => import('../components/admin/AdminLayout.vue'),
    meta: { title: '管理后台', layout: 'admin', requiresAuth: true, roles: ['ADMIN'] },
    children: [
      { path: '', component: () => import('../views/admin/Overview.vue'), meta: { title: '控制台概览' } },
      { path: 'contents', component: () => import('../views/admin/ContentReview.vue'), meta: { title: '内容审核' } },
      {
        path: 'creator-applications',
        component: () => import('../views/admin/CreatorApplications.vue'),
        meta: { title: '创作者申请' },
      },
      { path: 'comments', component: () => import('../views/admin/Comments.vue'), meta: { title: '评论管理' } },
      {
        path: 'skill-comments',
        component: () => import('../views/admin/SkillComments.vue'),
        meta: { title: 'Skill 评论管理' },
      },
      { path: 'skills', component: () => import('../views/admin/SkillManage.vue'), meta: { title: 'Skill 商城管理' } },
      { path: 'skills/new', component: () => import('../views/admin/SkillEdit.vue'), meta: { title: '新增 Skill' } },
      {
        path: 'skills/:id/edit',
        component: () => import('../views/admin/SkillEdit.vue'),
        meta: { title: '编辑 Skill' },
      },
      { path: 'users', component: () => import('../views/admin/Users.vue'), meta: { title: '用户管理' } },
      { path: 'categories', component: () => import('../views/admin/CategoryManage.vue'), meta: { title: '分类管理' } },
      { path: 'plans', component: () => import('../views/admin/Plans.vue'), meta: { title: '套餐管理' } },
    ],
  },

  /*
   * 404。
   * 改造前这里是 `redirect: '/'`，用户输错地址会莫名其妙回到首页，
   * 既看不出发生了什么，也没法自己纠正。现在给一个真正的 404 页面。
   */
  {
    path: '/:pathMatch(.*)*',
    component: () => import('../views/NotFound.vue'),
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  /*
   * history 模式（原来是 createWebHashHistory，地址是 /#/contents）。
   * hash 地址对分享、埋点、SEO 都不友好，成熟产品基本都用 history 模式。
   * 依赖服务端把未匹配的路径回退到 index.html —— nginx.conf 里已有
   * `try_files $uri $uri/ /index.html;`，无需额外配置。
   */
  history: createWebHistory(),
  routes,
  /** 切换路由回到页首；浏览器前进/后退时恢复原位置 */
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition ?? { top: 0 }
  },
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
  document.title = to.meta.title ? `${to.meta.title} · ContentHub` : 'ContentHub · 数字内容订阅平台'
})

export default router
