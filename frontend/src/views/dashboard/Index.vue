<template>
  <div class="qz-page page-dashboard">
    <!-- 欢迎栏 -->
    <el-card class="welcome qz-card mb-24" shadow="never">
      <div class="flex-between">
        <div>
          <h2 class="title mb-8">
            👋 Hi，{{ user.name || user.username || '同学' }}，
            欢迎回到<span class="text-primary"> 青知共享平台 </span>
          </h2>
          <p class="desc text-muted">
            当前身份：
            <el-tag size="small" effect="plain" :type="user.isAdmin ? 'danger': (user.isTeacher ? 'warning' : 'success')">
              {{ user.roleName || '普通用户' }}
            </el-tag>
            &nbsp;&nbsp;
            登录时间：{{ now }}
          </p>
        </div>
        <div class="hidden md:block">
          <el-button
            v-permission="['teacher','student']"
            type="primary"
            size="large"
            @click="$router.push('/resource/publish')"
          >
            <el-icon><Upload /></el-icon>&nbsp;发布资源
          </el-button>
          <el-button
            v-permission="'admin'"
            type="warning"
            size="large"
            class="ml-8"
            @click="$router.push('/admin/review')"
          >
            <el-icon><Check /></el-icon>&nbsp;去审核
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="mb-24">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <el-card class="stat qz-card" shadow="hover">
          <div class="flex-between">
            <div>
              <div class="stat-num">{{ c.value }}</div>
              <div class="stat-label text-muted mt-4">{{ c.label }}</div>
            </div>
            <el-icon :size="28" :color="c.color">
              <component :is="c.icon" />
            </el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 功能入口 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card class="qz-card" shadow="never">
          <template #header>
            <div class="flex-between">
              <strong>🧭 快捷入口</strong>
              <router-link class="text-primary text-sm" to="/resource/list">全部资源 →</router-link>
            </div>
          </template>
          <div class="quick-grid">
            <div
              v-for="q in quickLinks"
              :key="q.to"
              class="quick-item cursor-pointer rounded-8"
              @click="$router.push(q.to)"
            >
              <el-icon :size="22" color="var(--qz-primary)"><component :is="q.icon" /></el-icon>
              <span class="ml-8">{{ q.name }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="qz-card" shadow="never">
          <template #header><strong>📌 开发进度 / TODO</strong></template>
          <ul class="todo-list">
            <li>✅ 登录 / 注册页面</li>
            <li>✅ 侧边栏菜单（按角色过滤）</li>
            <li>✅ 头部用户下拉菜单</li>
            <li>⏳ 资源列表 / 搜索 / 筛选</li>
            <li>⏳ 资源详情 + 下载 + 收藏</li>
            <li>⏳ 资源发布 / 编辑 / 删除</li>
            <li>⏳ 我的资源 / 我的收藏 / 个人信息 / 改密</li>
            <li>⏳ 管理后台：用户管理 / 资源审核 / 资源管理 / Excel 导入</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/userStore'
import { formatDateTime } from '@/utils/format'

const user = useUserStore()
const now = ref(formatDateTime(Date.now()))
onMounted(() => {
  now.value = formatDateTime(Date.now())
})

/* 占位 mock 数据，等下一批次对接后端真实统计 API */
const cards = computed(() => [
  { label: '平台资源总数', value: '238', icon: 'Document',  color: 'var(--qz-primary)' },
  { label: '已通过审核',   value: '201', icon: 'Checked',   color: 'var(--qz-review-pass)' },
  { label: '今日下载次数', value: '1,236', icon: 'Download', color: 'var(--qz-role-teacher)' },
  { label: '平台用户数',   value: '1,024', icon: 'UserFilled', color: 'var(--qz-role-admin)' }
])

const quickLinks = computed(() => {
  const arr = [
    { name: '资源中心', to: '/resource/list',    icon: 'Compass' },
    { name: '发布资源', to: '/resource/publish', icon: 'Upload' },
    { name: '我的收藏', to: '/profile/favorites',icon: 'Star' },
    { name: '我的资源', to: '/profile/resources',icon: 'Folder' },
    { name: '个人信息', to: '/profile/info',     icon: 'User' },
    { name: '修改密码', to: '/profile/password', icon: 'Lock' }
  ]
  if (user.isAdmin) {
    arr.push(
      { name: '用户管理',   to: '/admin/users',    icon: 'Avatar' },
      { name: '资源审核',   to: '/admin/review',   icon: 'Check' },
      { name: '资源管理',   to: '/admin/resource', icon: 'Operation' },
      { name: '批量导入',   to: '/admin/import',   icon: 'UploadFilled' }
    )
  }
  return arr
})
</script>

<style scoped>
.title { margin: 0; font-size: 22px; }
.desc  { margin: 0; }
.text-muted { color: var(--qz-text-secondary); }
.text-primary{ color: var(--qz-primary); }
.text-sm{ font-size: 13px; }
.mb-8  { margin-bottom: 8px; }
.mb-24 { margin-bottom: 24px; }
.mt-4  { margin-top: 4px; }
.ml-8  { margin-left: 8px; }
.rounded-8{ border-radius: 8px; }
.hidden.md-block{ display:none } @media (min-width:768px){ .hidden.md-block{ display:block } }

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: var(--qz-text-primary);
  letter-spacing: 1px;
}
.stat-label {
  font-size: 13px;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.quick-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  border: 1px solid var(--qz-border-light);
  background: #fff;
  transition: all .18s ease;
}
.quick-item:hover {
  border-color: var(--qz-primary);
  color: var(--qz-primary);
  transform: translateY(-1px);
}
.todo-list {
  padding-left: 20px;
  margin: 0;
  line-height: 2;
  color: var(--qz-text-secondary);
}
</style>
