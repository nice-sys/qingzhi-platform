import Layout from '@/layout/Layout.vue'
import { ROLE } from '@/utils/permission'

const ADMIN   = [ROLE.ADMIN]
const TEACHER = [ROLE.TEACHER]
const STUDENT = [ROLE.STUDENT]
const LOGIN   = [ROLE.ADMIN, ROLE.TEACHER, ROLE.STUDENT] // 所有登录用户

/**
 * 路由表（与 PRD 3.3.3 目录结构 100% 对齐）
 * meta.roles = 允许访问的角色 code 数组；空数组 = 不拦截（白名单）
 * meta.title = 侧边栏/面包屑标题
 * meta.icon  = Element Plus 图标名
 * meta.sidebar = 是否出现在侧边栏（默认 true）
 */
export const routes = [
  /* ---------- 白名单：登录注册页 ---------- */
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', roles: [], sidebar: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', roles: [], sidebar: false }
  },

  /* ---------- 主布局内的页面 ---------- */
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    meta: { title: '首页', icon: 'HomeFilled', roles: LOGIN, sidebarSingleChild: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: '首页', icon: 'HomeFilled', roles: LOGIN }
      }
    ]
  },
  {
    path: '/resource',
    component: Layout,
    redirect: '/resource/list',
    meta: { title: '资源中心', icon: 'Folder', roles: LOGIN },
    children: [
      {
        path: 'list',
        name: 'ResourceList',
        component: () => import('@/views/resource/ResourceList.vue'),
        meta: { title: '资源列表', icon: 'List', roles: LOGIN }
      },
      {
        path: 'publish',
        name: 'ResourcePublish',
        component: () => import('@/views/resource/ResourcePublish.vue'),
        meta: { title: '发布资源', icon: 'UploadFilled', roles: [...TEACHER, ...STUDENT] }
      },
      {
        path: 'update/:id',
        name: 'ResourceUpdate',
        component: () => import('@/views/resource/ResourceUpdate.vue'),
        meta: { title: '修改资源', roles: [...TEACHER, ...STUDENT], sidebar: false }
      },
      {
        path: ':id',
        name: 'ResourceDetail',
        component: () => import('@/views/resource/ResourceDetail.vue'),
        meta: { title: '资源详情', roles: LOGIN, sidebar: false }
      }
    ]
  },
  {
    path: '/profile',
    component: Layout,
    redirect: '/profile/info',
    meta: { title: '个人中心', icon: 'UserFilled', roles: LOGIN },
    children: [
      {
        path: 'info',
        name: 'ProfileInfo',
        component: () => import('@/views/profile/Info.vue'),
        meta: { title: '个人信息', icon: 'User', roles: LOGIN }
      },
      {
        path: 'password',
        name: 'ProfilePassword',
        component: () => import('@/views/profile/ChangePassword.vue'),
        meta: { title: '修改密码', icon: 'Lock', roles: LOGIN }
      },
      {
        path: 'resources',
        name: 'MyResources',
        component: () => import('@/views/profile/MyResources.vue'),
        meta: { title: '我的资源', icon: 'Document', roles: [...TEACHER, ...STUDENT] }
      },
      {
        path: 'favorites',
        name: 'MyFavorites',
        component: () => import('@/views/profile/MyFavorites.vue'),
        meta: { title: '我的收藏', icon: 'StarFilled', roles: [...TEACHER, ...STUDENT] }
      }
    ]
  },
  {
    path: '/admin',
    component: Layout,
    redirect: '/admin/users',
    meta: { title: '管理后台', icon: 'Setting', roles: ADMIN },
    children: [
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理', icon: 'Avatar', roles: ADMIN }
      },
      {
        path: 'review',
        name: 'AdminReview',
        component: () => import('@/views/admin/ResourceReview.vue'),
        meta: { title: '资源审核', icon: 'CircleCheckFilled', roles: ADMIN }
      },
      {
        path: 'resources',
        name: 'AdminResources',
        component: () => import('@/views/admin/ResourceManage.vue'),
        meta: { title: '资源管理', icon: 'Files', roles: ADMIN }
      },
      {
        path: 'import',
        name: 'AdminImport',
        component: () => import('@/views/admin/ExcelImport.vue'),
        meta: { title: '批量导入', icon: 'Upload', roles: ADMIN }
      }
    ]
  },

  /* ---------- 404 ---------- */
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/dashboard',
    meta: { roles: [], sidebar: false }
  }
]

export default routes
