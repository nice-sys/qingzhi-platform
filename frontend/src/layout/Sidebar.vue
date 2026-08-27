<template>
  <div class="sidebar h-full flex-col">
    <!-- Logo 区 -->
    <div class="sidebar-logo flex-center cursor-pointer" @click="$router.push('/dashboard')">
      <el-icon :size="28" color="#26b89a"><CollectionTag /></el-icon>
      <span v-if="!app.sidebarCollapsed" class="sidebar-logo-title ml-8">青知共享</span>
    </div>

    <!-- 菜单 -->
    <el-scrollbar class="sidebar-scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="app.sidebarCollapsed"
        :collapse-transition="false"
        background-color="#1f2d3d"
        text-color="#c0ccda"
        active-text-color="#26b89a"
        router
        unique-opened
      >
        <template v-for="item in menuList" :key="item.path">
          <!-- 有子菜单 -->
          <template v-if="item.children && item.children.length">
            <!-- 仅 1 个可见子菜单项 + meta.sidebarSingleChild=true → 直接平铺为单个菜单，避免多余嵌套 -->
            <el-menu-item
              v-if="
                item.meta && item.meta.sidebarSingleChild === true
                && visibleChildren(item).length === 1
                && hasRole(visibleChildren(item)[0])
              "
              :index="resolve(item.path, visibleChildren(item)[0].path)"
            >
              <el-icon v-if="item.meta.icon"><component :is="item.meta.icon" /></el-icon>
              <template #title>{{ item.meta.title }}</template>
            </el-menu-item>
            <!-- 多子菜单 / 无子菜单标记 → 标准 sub-menu 嵌套 -->
            <el-sub-menu v-else :index="item.path">
              <template #title>
                <el-icon v-if="item.meta && item.meta.icon"><component :is="item.meta.icon" /></el-icon>
                <span>{{ item.meta ? item.meta.title : '' }}</span>
              </template>
              <template v-for="ch in item.children" :key="ch.path">
                <el-menu-item
                  v-if="ch.meta && ch.meta.sidebar !== false && hasRole(ch)"
                  :index="resolve(item.path, ch.path)"
                >
                  <el-icon v-if="ch.meta.icon"><component :is="ch.meta.icon" /></el-icon>
                  <template #title>{{ ch.meta.title }}</template>
                </el-menu-item>
              </template>
            </el-sub-menu>
          </template>
          <!-- 无嵌套 -->
          <el-menu-item
            v-else-if="item.meta && item.meta.sidebar !== false && hasRole(item)"
            :index="item.path"
          >
            <el-icon v-if="item.meta.icon"><component :is="item.meta.icon" /></el-icon>
            <template #title>{{ item.meta.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { routes } from '@/router/routes'
import { useAppStore } from '@/stores/appStore'
import { useUserStore } from '@/stores/userStore'
import { roleMatchesAny } from '@/utils/permission'

const app = useAppStore()
const user = useUserStore()
const route = useRoute()

/* 只取 Layout 内定义的顶层路由（去掉白名单 login/register / 404 等） */
const menuList = computed(() => routes.filter(r => {
  if (!r.component) return false
  if (!r.meta) return false
  return !Array.isArray(r.meta.roles) || r.meta.roles.length > 0 // 不是白名单
}))

const activeMenu = computed(() => route.path)

function hasRole(routeDef) {
  const roles = routeDef.meta && routeDef.meta.roles
  if (!roles || roles.length === 0) return true
  return roleMatchesAny(user.role, roles)
}
function visibleChildren(item) {
  if (!item || !Array.isArray(item.children)) return []
  return item.children.filter(ch => ch.meta && ch.meta.sidebar !== false)
}
function resolve(parent, child) {
  if (child.startsWith('/')) return child
  return (parent.endsWith('/') ? parent : parent + '/') + child
}
</script>

<style scoped>
.sidebar {
  width: 100%;
  height: 100%;
  display: flex;
}
.sidebar-logo {
  height: var(--qz-header-h);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.sidebar-logo-title {
  white-space: nowrap;
}
.sidebar-scroll {
  flex: 1 1 auto;
  min-height: 0;
}
:deep(.el-menu) {
  border-right: none;
}
:deep(.el-menu-item.is-active) {
  background: rgba(47, 122, 107, 0.14);
}
.ml-8 { margin-left: 8px; }
</style>
