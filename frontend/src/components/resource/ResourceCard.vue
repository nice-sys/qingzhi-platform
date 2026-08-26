<template>
  <el-card
    class="resource-card qz-card h-full cursor-pointer"
    :class="{ 'is-favorited': favorited, 'is-pass': Number(data.reviewStatus) === 1 }"
    shadow="hover"
    @click="$emit('click', data)"
  >
    <!-- 顶部：类型标签 + 收藏 -->
    <div class="flex-between mb-8">
      <div class="flex gap-4 items-center flex-wrap">
        <el-tag size="small" effect="plain" :type="typeTag">{{ data.type || 'other' }}</el-tag>
        <el-tag v-if="data.category" size="small" type="info" effect="plain" round class="cat-tag">
          {{ data.category }}
        </el-tag>
      </div>
      <div class="flex items-center gap-4">
        <el-tag
          v-if="data.uploaderRole != null"
          size="small"
          effect="plain"
          :color="roleTagColor(data.uploaderRole)"
        >{{ roleTagLabel(data.uploaderRole) }}</el-tag>
        <StatusTag :status="data.reviewStatus" kind="review" />
      </div>
    </div>

    <!-- 标题 -->
    <h4 class="title truncate mb-8" :title="data.title">{{ data.title || '未命名资源' }}</h4>
    <p class="desc text-muted truncate-2 mb-12">{{ data.description || '暂无描述' }}</p>

    <!-- 元信息 -->
    <div class="meta text-muted text-sm flex-between mb-12">
      <span class="truncate" :title="data.uploaderName || data.uploaderUsername">
        <el-icon><User /></el-icon>&nbsp;{{ data.uploaderName || data.uploaderUsername || '-' }}
      </span>
      <span>{{ formatDate(data.createdAt) }}</span>
    </div>

    <!-- 底部：数据 + 操作 -->
    <el-divider class="my-0" />
    <div class="flex-between mt-12">
      <div class="stats text-muted text-sm">
        <span class="mr-12"><el-icon><Download /></el-icon>&nbsp;{{ formatCount(data.downloadCount) }}</span>
        <span><el-icon><Star /></el-icon>&nbsp;{{ formatCount(data.favoriteCount) }}</span>
      </div>
      <div class="actions flex gap-4">
        <el-button
          link
          type="primary"
          @click.stop="$emit('quickDownload', data)"
          :disabled="Number(data.reviewStatus) !== 1"
          :title="Number(data.reviewStatus) !== 1 ? '审核通过后可下载' : '立即下载'"
        >
          <el-icon><Download /></el-icon>
        </el-button>
        <el-button
          v-if="showFavorite"
          link
          :type="favorited ? 'danger' : 'primary'"
          @click.stop="$emit('toggleFavorite', data)"
          :title="favorited ? '取消收藏' : '收藏'"
        >
          <el-icon><Star :fill="favorited ? 'currentColor' : 'none'" /></el-icon>
        </el-button>
        <el-button link type="primary" @click.stop="$emit('click', data)">
          详情
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { formatDateTime, formatDownloadCount } from '@/utils/format'
import { TYPE_TAG_TYPE } from '@/utils/constants'
import { ROLE_NAME } from '@/utils/permission'

const props = defineProps({
  data:          { type: Object, default: () => ({}) },
  favorited:     { type: Boolean, default: false },
  showFavorite:  { type: Boolean, default: true }
})
defineEmits(['click', 'toggleFavorite', 'quickDownload'])

const formatDate = formatDateTime
const formatCount = formatDownloadCount

const typeTag = computed(() => {
  const t = (props.data.type || '').toLowerCase()
  return TYPE_TAG_TYPE[t] || TYPE_TAG_TYPE.other || 'info'
})

function roleTagColor(role) {
  const r = Number(role)
  return r === 0 ? 'var(--qz-role-admin)'
       : r === 1 ? 'var(--qz-role-teacher)'
       : 'var(--qz-role-student)'
}
function roleTagLabel(role) {
  const r = Number(role)
  return ROLE_NAME[r] || ''
}
</script>

<style scoped>
.title { font-size: 16px; font-weight: 600; color: var(--qz-text-primary); }
.truncate { overflow:hidden; text-overflow: ellipsis; white-space: nowrap; }
.truncate-2 {
  display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical;
  overflow:hidden; min-height: 42px;
}
.h-full { height: 100%; }
.mb-8  { margin-bottom: 8px; }
.mb-12 { margin-bottom: 12px; }
.mt-12 { margin-top: 12px; }
.mr-12 { margin-right: 12px; }
.my-0  { margin-top: 0; margin-bottom: 0; }
.gap-4 { gap: 4px; }
.stats > span { display: inline-flex; align-items: center; }

.cat-tag { opacity: .88; }
.is-favorited {
  border-color: var(--el-color-danger-light-7);
  box-shadow: 0 0 0 1px rgba(var(--el-color-danger-rgb), 0.25);
}
.is-favorited:hover {
  border-color: var(--el-color-danger-light-5);
}
.is-pass {
  border-left: 3px solid var(--qz-primary);
}
.flex-wrap { flex-wrap: wrap; }
</style>
