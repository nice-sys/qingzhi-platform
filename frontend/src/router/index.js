import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { useUserStore } from '@/stores/userStore'
import { roleMatchesAny } from '@/utils/permission'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, saved) {
    if (saved) return saved
    return { top: 0 }
  }
})

/**
 * 全局前置守卫：
 * 1. 白名单路由（meta.roles 空数组）直接放行
 * 2. 未登录：跳 /login?redirect=<原路径>
 * 3. 已登录：检查路由 meta.roles，匹配不到跳 /dashboard 并弹提示
 */
router.beforeEach((to, _from, next) => {
  const user = useUserStore()
  const roles = (to.meta && to.meta.roles instanceof Array) ? to.meta.roles : null

  // 白名单：无需登录（roles=[]）
  if (roles && roles.length === 0) {
    // 已登录用户访问 login/register 这种白名单页 → 直接去首页，避免重复登录
    if (user.isLoggedIn && (to.path === '/login' || to.path === '/register')) {
      return next('/dashboard')
    }
    return next()
  }

  // 需登录
  if (!user.isLoggedIn) {
    ElMessage.warning('请先登录')
    return next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  }

  // 角色权限：meta.roles 未设置则默认所有登录用户可进；否则匹配
  if (roles && roles.length > 0) {
    if (!roleMatchesAny(user.role, roles)) {
      ElMessage.error('您没有访问该页面的权限')
      return next('/dashboard')
    }
  }

  next()
})

/* 标题随路由变化 */
router.afterEach((to) => {
  const t = to.meta && to.meta.title
  const base = '青知共享平台'
  document.title = t ? `${t} - ${base}` : base
})

export default router
