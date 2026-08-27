<template>
  <div class="header h-full w-full flex-between px-16">
    <!-- 左侧：折叠按钮 + 面包屑 -->
    <div class="flex items-center">
      <el-button
        text
        size="large"
        class="mr-12"
        @click="app.toggleSidebar()"
      >
        <el-icon :size="18">
          <component :is="app.sidebarCollapsed ? 'Expand' : 'Fold'" />
        </el-icon>
      </el-button>

      <el-breadcrumb separator="/">
        <el-breadcrumb-item
          v-for="(b, i) in breadcrumbs"
          :key="i"
          :to="b.to || undefined"
        >{{ b.title }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 右侧：用户下拉 -->
    <div class="flex items-center">
      <el-tooltip content="GitHub 式占位提醒：点击头像看操作" placement="bottom">
        <el-dropdown trigger="click" @command="onCommand">
          <div class="header-user flex items-center cursor-pointer px-8 py-4 rounded-8 hover:bg-[#f5f7fa]">
            <el-avatar :size="34" :style="{ background: avatarBg, color: '#fff' }">
              <img v-if="avatarUrl" :src="avatarUrl" style="width:100%;height:100%;object-fit:cover;display:block;" />
              <el-icon v-else><User /></el-icon>
            </el-avatar>
            <span class="ml-8 mr-4 truncate" style="max-width:140px">
              {{ user.name || user.username || '未登录' }}
            </span>
            <el-tag v-if="user.roleName" size="small" :color="tagBg" effect="plain" bordered>
              {{ user.roleName }}
            </el-tag>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="info">
                <el-icon><User /></el-icon>&nbsp;个人信息
              </el-dropdown-item>
              <el-dropdown-item command="password">
                <el-icon><Lock /></el-icon>&nbsp;修改密码
              </el-dropdown-item>
              <el-dropdown-item command="myres" v-if="!user.isAdmin">
                <el-icon><Document /></el-icon>&nbsp;我的资源
              </el-dropdown-item>
              <el-dropdown-item command="drafts" v-if="!user.isAdmin">
                <el-icon><EditPen /></el-icon>&nbsp;我的草稿
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>&nbsp;退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/appStore'
import { useUserStore } from '@/stores/userStore'

const app = useAppStore()
const user = useUserStore()
const router = useRouter()
const route = useRoute()

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(m => m.meta && m.meta.title)
  const arr = matched.map(m => ({ title: m.meta.title, to: null }))
  // 最后一项不可点
  if (arr.length) arr[arr.length - 1].to = null
  return arr.length ? arr : [{ title: '首页', to: null }]
})

const bg = computed(() => {
  if (user.isAdmin) return 'var(--qz-role-admin)'
  if (user.isTeacher) return 'var(--qz-role-teacher)'
  return 'var(--qz-role-student)'
})
const avatarBg = bg
const tagBg = computed(() => bg.value)
const avatarUrl = computed(() => {
  const i = user.userInfo || {}
  return i.avatarUrl || i.avatarURL || i.avatar || ''
})

function onCommand(cmd) {
  switch (cmd) {
    case 'info':     router.push('/profile/info'); break
    case 'password': router.push('/profile/password'); break
    case 'myres':    router.push('/profile/resources'); break
    case 'drafts':   router.push('/profile/drafts'); break
    case 'logout':   doLogout(); break
  }
}
async function doLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  } catch (_) { return }
  user.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.rounded-8 { border-radius: 8px; }
.h-full { height: 100%; }
.w-full { width: 100%; }
.px-8 { padding-left: 8px; padding-right: 8px; }
.px-16 { padding-left: 16px; padding-right: 16px; }
.py-4 { padding-top: 4px; padding-bottom: 4px; }
.mr-4 { margin-right: 4px; }
.mr-12 { margin-right: 12px; }
.ml-8 { margin-left: 8px; }
</style>
