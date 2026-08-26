<template>
  <div v-if="!total || total === 0" class="empty-wrap flex-col items-center py-32">
    <el-empty :description="desc || '暂无数据'">
      <template #image>
        <el-icon v-if="icon" :size="56" color="#c7ccd1"><component :is="icon" /></el-icon>
      </template>
      <slot name="desc">{{ desc || '暂无数据' }}</slot>
      <div class="empty-actions mt-16">
        <slot name="action">
          <el-button v-if="withBack" @click="$router.back()">返回上一页</el-button>
        </slot>
      </div>
    </el-empty>
  </div>
  <slot v-else name="list"></slot>
</template>

<script setup>
defineProps({
  total:    { type: Number,  default: 0 },
  desc:     { type: String,  default: '' },
  icon:     { type: String,  default: 'FolderOpened' },
  withBack: { type: Boolean, default: false }
})
</script>

<style scoped>
.empty-wrap { min-height: 300px; }
.mt-16 { margin-top: 16px; }
</style>
