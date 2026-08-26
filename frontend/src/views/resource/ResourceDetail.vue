<template>
  <div class="qz-page page-resource-detail">
    <!-- 返回 + 面包屑 -->
    <div class="mb-16">
      <el-button link type="primary" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>&nbsp;返回上一页
      </el-button>
    </div>

    <el-empty v-if="loading && !detail.id" description="资源加载中..." />

    <div v-else-if="!detail.id">
      <EmptyState :total="0" desc="资源不存在或已被删除" icon="Warning" with-back />
    </div>

    <template v-else>
      <!-- 顶部标题卡 -->
      <el-card class="qz-card mb-16" shadow="never">
        <div class="flex-between gap-16">
          <div class="flex-1">
            <div class="flex gap-8 items-center mb-12 flex-wrap">
              <el-tag size="large" effect="plain" :type="typeTag">
                <el-icon class="mr-4"><component :is="typeIcon" /></el-icon>
                {{ typeLabel }}
              </el-tag>
              <StatusTag :status="detail.reviewStatus" kind="review" />
              <el-tag v-if="detail.category" size="large">{{ detail.category }}</el-tag>
              <el-tag v-for="t in tagList" :key="t" size="small" type="success" effect="plain">
                #{{ t }}
              </el-tag>
            </div>
            <h1 class="mb-12 title">{{ detail.title }}</h1>
            <div class="meta text-muted text-sm flex gap-16 flex-wrap items-center">
              <div class="flex items-center gap-8">
                <!-- 上传者头像 -->
                <div class="uploader-avatar">
                  <img
                    v-if="uploaderAvatar"
                    :src="uploaderAvatar"
                    alt="avatar"
                    class="avatar-img"
                    @error="uploaderAvatar=''"
                  />
                  <el-avatar v-else :size="28" :style="{ backgroundColor: roleBgColor(detail.uploaderRole) }">
                    {{ (detail.uploaderName || detail.uploaderUsername || 'U').charAt(0) }}
                  </el-avatar>
                </div>
                <span>
                  <span class="text-muted"><el-icon><User /></el-icon>&nbsp;上传者：</span>
                  <b class="text-primary">{{ detail.uploaderName || detail.uploaderUsername || '-' }}</b>
                  <el-tag
                    v-if="detail.uploaderRole != null"
                    size="small"
                    effect="plain"
                    :color="roleTagColor(detail.uploaderRole)"
                    class="ml-4"
                  >{{ ROLE_NAME[Number(detail.uploaderRole)] || '' }}</el-tag>
                  <span v-if="detail.uploaderId" class="text-muted"> (ID: {{ detail.uploaderId }})</span>
                </span>
              </div>
              <span><el-icon><Clock /></el-icon>&nbsp;上传：{{ formatTime(detail.createdAt) }}</span>
              <span><el-icon><EditPen /></el-icon>&nbsp;更新：{{ formatTime(detail.updatedAt) }}</span>
            </div>
          </div>
          <div class="right-stats hidden md:flex-col items-end" style="flex:0 0 auto">
            <div class="text-right">
              <div class="stat-num">{{ formatCount(detail.downloadCount) }}</div>
              <div class="text-muted text-sm">下载次数</div>
            </div>
            <div class="text-right mt-8">
              <div class="stat-num">{{ formatCount(detail.favoriteCount) }}</div>
              <div class="text-muted text-sm">收藏</div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 审核状态提醒 -->
      <el-alert
        v-if="Number(detail.reviewStatus) === REVIEW.PENDING"
        type="warning"
        show-icon
        :closable="false"
        class="mb-16"
      >
        <template #title>
          🕒 资源正在审核中：管理员审核通过后，所有师生均可下载；目前仅你自己和管理员可见。
        </template>
        <template #default>
          如需加速审核，可联系管理员通过【后台 → 资源审核】处理。
        </template>
      </el-alert>
      <el-alert
        v-else-if="Number(detail.reviewStatus) === REVIEW.REJECT"
        type="error"
        show-icon
        :closable="false"
        class="mb-16"
      >
        <template #title>
          ❌ 审核未通过
          <span v-if="detail.reviewRemark || detail.rejectReason">：{{ detail.reviewRemark || detail.rejectReason }}</span>
        </template>
        <template #default v-if="canEdit">
          点击右上角【编辑】按钮修改后提交，会自动进入重新审核队列。
        </template>
      </el-alert>

      <el-row :gutter="16">
        <!-- 左侧描述 -->
        <el-col :span="16">
          <el-card class="qz-card mb-16" shadow="never">
            <template #header><strong>📄 资源描述</strong></template>
            <p class="text-muted mb-0 desc-content">
              {{ detail.description || '（暂无描述）' }}
            </p>
          </el-card>

          <!-- 文件信息 -->
          <el-card class="qz-card" shadow="never">
            <template #header><strong>📎 文件信息</strong></template>
            <el-descriptions :column="2" size="default" border>
              <el-descriptions-item label="文件名">
                <span class="truncate block" style="max-width:420px">
                  {{ detail.originalName || '-' }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="文件大小">{{ formatSize(detail.fileSize) }}</el-descriptions-item>
              <el-descriptions-item label="资源ID">{{ detail.id }}</el-descriptions-item>
              <el-descriptions-item label="存储ID">{{ detail.storageId || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <!-- 右侧操作栏 -->
        <el-col :span="8">
          <el-card class="qz-card sticky-card" shadow="hover">
            <template #header><strong>⚙️ 操作</strong></template>

            <div class="action-group">
              <el-button
                type="primary"
                size="large"
                class="w-full mb-8"
                :disabled="Number(detail.reviewStatus) !== REVIEW.PASS"
                :loading="downloading"
                @click="doDownload"
              >
                <el-icon><Download /></el-icon>&nbsp;
                {{ Number(detail.reviewStatus) !== REVIEW.PASS ? '审核通过后可下载' : '立即下载' }}
              </el-button>
              <el-button
                size="large"
                class="w-full mb-8"
                :type="favorited ? 'danger' : 'default'"
                @click="toggleFav"
              >
                <el-icon><Star :fill="favorited ? 'currentColor' : 'none'" /></el-icon>&nbsp;
                {{ favorited ? '已收藏（点击取消）' : '收藏资源' }}
              </el-button>
              <el-button
                v-if="canEdit"
                size="large"
                class="w-full mb-8"
                @click="$router.push(`/resource/${detail.id}/update`)"
              >
                <el-icon><Edit /></el-icon>&nbsp;编辑资源
              </el-button>
              <el-popconfirm
                v-if="canEdit"
                title="删除后无法恢复，确认删除此资源？"
                @confirm="del"
              >
                <template #reference>
                  <el-button size="large" type="danger" class="w-full" plain>
                    <el-icon><Delete /></el-icon>&nbsp;删除资源
                  </el-button>
                </template>
              </el-popconfirm>
            </div>

            <el-divider />

            <div class="text-muted text-sm tips">
              <p class="mb-4">💡 小提示：</p>
              <ul class="pl-16 mb-0" style="line-height:2">
                <li>相同文件支持秒传，不会重复占用服务器空间</li>
                <li>发现违规资源？可联系管理员移除</li>
                <li>下载次数实时统计，以实际点击下载为准</li>
              </ul>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { onMounted, computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import StatusTag  from '@/components/common/StatusTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import {
  resourceDetail, downloadResource, deleteResource
} from '@/api/resource'
import { checkFavorite, addFavorite, removeFavorite } from '@/api/favorite'
import { useUserStore } from '@/stores/userStore'
import { formatDateTime, formatFileSize, formatDownloadCount } from '@/utils/format'
import {
  RESOURCE_TYPE_LABEL, TYPE_TAG_TYPE, TYPE_ICON, REVIEW
} from '@/utils/constants'
import { ROLE_NAME } from '@/utils/permission'

const route = useRoute()
const router = useRouter()
const user  = useUserStore()
const loading = ref(false)
const downloading = ref(false)
const detail = ref({})
const favorited = ref(false)
const uploaderAvatar = ref('')

const formatTime  = formatDateTime
const formatSize  = formatFileSize
const formatCount = formatDownloadCount

const id = computed(() => Number(route.params.id) || 0)

/* 权限：管理员 或 上传者本人 可编辑/删除 */
const canEdit = computed(() => {
  if (!detail.value.id) return false
  if (user.isAdmin) return true
  return !!(user.userId && detail.value.uploaderId && user.userId === detail.value.uploaderId)
})
const tagList = computed(() => (detail.value.tags || '').split(',').filter(Boolean))

/* 类型展示：优先用后端给的 type，其次按原始文件名推断 */
const typeKey = computed(() => {
  const t = (detail.value.type || '').toLowerCase()
  if (t) return t
  const n = detail.value.originalName || ''
  return n ? ((n.split('.').pop() || '').toLowerCase()) : ''
})
const typeLabel = computed(() => RESOURCE_TYPE_LABEL[typeKey.value] || '其他')
const typeTag   = computed(() => TYPE_TAG_TYPE[typeKey.value] || TYPE_TAG_TYPE.other || 'info')
const typeIcon  = computed(() => TYPE_ICON[typeKey.value] || TYPE_ICON.other || 'Paperclip')

function roleTagColor(role) {
  const r = Number(role)
  return r === 0 ? 'var(--qz-role-admin)'
       : r === 1 ? 'var(--qz-role-teacher)'
       : 'var(--qz-role-student)'
}
function roleBgColor(role) {
  const r = Number(role)
  if (r === 0) return '#909399'
  if (r === 1) return 'var(--qz-role-teacher)'
  return 'var(--qz-role-student)'
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/resource/list')
}

async function fetch() {
  loading.value = true
  uploaderAvatar.value = ''
  try {
    detail.value = await resourceDetail(id.value)
    uploaderAvatar.value = detail.value.uploaderAvatarUrl
      || detail.value.uploaderAvatarURL
      || detail.value.uploaderAvatar
      || ''
    if (user.isLoggedIn) {
      const r = await checkFavorite(id.value)
      favorited.value = !!(r && r.favorited)
    }
  } catch (_) {} finally { loading.value = false }
}
async function doDownload() {
  if (!user.isLoggedIn) {
    ElMessage.warning('请先登录后下载')
    router.replace({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  downloading.value = true
  try {
    await downloadResource(id.value, detail.value.originalName)
  } catch (_) {} finally { downloading.value = false }
}
async function toggleFav() {
  if (!user.isLoggedIn) {
    ElMessage.warning('请先登录后收藏')
    router.replace({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    if (favorited.value) {
      await removeFavorite(id.value)
      favorited.value = false
      if (typeof detail.value.favoriteCount === 'number') detail.value.favoriteCount -= 1
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(id.value)
      favorited.value = true
      if (typeof detail.value.favoriteCount === 'number') detail.value.favoriteCount += 1
      ElMessage.success('收藏成功')
    }
  } catch (_) {}
}
async function del() {
  try {
    await deleteResource(id.value)
    ElMessage.success('已删除资源')
    const fallback = user.isAdmin ? '/admin/resource' : '/profile/resources'
    router.replace(fallback)
  } catch (_) {}
}

onMounted(fetch)
</script>

<style scoped>
.title { font-size: 24px; margin: 0; font-weight: 700; color: var(--qz-text-primary); line-height:1.5; }
.stat-num { font-size: 26px; font-weight: 700; color: var(--qz-primary); }
.meta span { display: inline-flex; align-items: center; color: var(--qz-text-secondary); }
.text-muted { color: var(--qz-text-secondary); }
.text-primary { color: var(--qz-primary); }
.text-sm { font-size: 13px; }
.text-right { text-align: right; }
.mb-0  { margin-bottom: 0; }
.mb-4  { margin-bottom: 4px; }
.mb-8  { margin-bottom: 8px; }
.mb-12 { margin-bottom: 12px; }
.mb-16 { margin-bottom: 16px; }
.mt-8  { margin-top: 8px; }
.mr-4  { margin-right: 4px; }
.ml-4  { margin-left: 4px; }
.ml-8  { margin-left: 8px; }
.pl-16 { padding-left: 16px; }
.gap-8 { gap: 8px; }
.gap-16{ gap: 16px; }
.w-full{ width: 100%; }
.flex-wrap { flex-wrap: wrap; }
.flex-1 { flex: 1 1 auto; }
.items-end { align-items: flex-end; }
.flex-col { display: flex; flex-direction: column; }
.hidden.md\:flex-col { display: none; }
@media (min-width: 768px) { .hidden.md\:flex-col { display: flex; } }

.desc-content {
  white-space: pre-wrap;
  line-height: 2;
  color: var(--qz-text-primary);
  font-size: 14px;
}
.sticky-card { position: sticky; top: 12px; }
.action-group { display: flex; flex-direction: column; gap: 4px; }
.tips ul { margin: 0; }

.uploader-avatar {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.uploader-avatar .avatar-img {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
}
.items-center { align-items: center; }
</style>
