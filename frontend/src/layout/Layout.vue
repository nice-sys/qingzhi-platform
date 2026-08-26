<template>
  <el-container class="layout-root h-full">
    <!-- 侧边栏 -->
    <el-aside
      :width="app.sidebarCollapsed ? '64px' : '220px'"
      class="layout-aside transition-all"
    >
      <Sidebar />
    </el-aside>

    <!-- 右侧主区 -->
    <el-container class="layout-main flex-col">
      <el-header class="layout-header">
        <Header />
      </el-header>
      <el-main class="layout-main-content p-0">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import { useAppStore } from '@/stores/appStore'

const app = useAppStore()
</script>

<style scoped>
.layout-root {
  width: 100vw;
  height: 100vh;
  background: var(--qz-bg-page);
  overflow: hidden;
}
.layout-aside {
  background: var(--qz-bg-sidebar);
  color: #e5e7eb;
  transition: width .2s ease;
}
.layout-header {
  height: var(--qz-header-h);
  line-height: var(--qz-header-h);
  padding: 0;
  background: #fff;
  border-bottom: 1px solid var(--qz-border-light);
  box-shadow: 0 1px 4px 0 rgba(0,0,0,0.04);
  z-index: 10;
}
.layout-main-content {
  overflow: auto;
  background: var(--qz-bg-page);
}
.transition-all { transition: all .2s ease; }
.fade-enter-active, .fade-leave-active { transition: opacity .2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
