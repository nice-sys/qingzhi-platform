<template>
  <div class="file-upload qz-card p-16 rounded-8">
    <el-upload
      ref="uploadRef"
      drag
      :auto-upload="false"
      :file-list="fileList"
      :limit="limit"
      :on-change="onChange"
      :on-remove="onRemove"
      :before-upload="beforeUpload"
      multiple
    >
      <el-icon class="el-icon--upload" :size="40"><UploadFilled /></el-icon>
      <div class="el-upload__text mt-8">
        将文件拖到此处，或<em class="text-primary">点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip text-muted text-sm mt-8">
          单文件 <= {{ formatSize(maxSize) }}，支持 PDF/PPT/Word/Excel/图片/压缩包 等；
          相同文件会自动秒传。
        </div>
      </template>
    </el-upload>

    <div v-if="uploading" class="mt-12">
      <el-progress :percentage="progress" :status="progress === 100 ? 'success' : ''" />
    </div>

    <div class="mt-16 flex gap-8">
      <el-button type="primary" :disabled="!fileList.length || uploading" @click="startUpload" :loading="uploading">
        <el-icon><Upload /></el-icon>&nbsp;上传选中的文件
      </el-button>
      <el-button :disabled="!fileList.length || uploading" @click="clearAll">清空列表</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/file'
import { formatFileSize } from '@/utils/format'

const props = defineProps({
  limit:   { type: Number, default: 1 },
  maxSize: { type: Number, default: 200 * 1024 * 1024 } // 默认200MB
})
const emit = defineEmits(['success', 'remove'])
const formatSize = formatFileSize

const uploadRef = ref(null)
const fileList = ref([])
const uploading = ref(false)
const progress  = ref(0)

function beforeUpload(file) {
  if (file.size > props.maxSize) {
    ElMessage.error(`文件超过最大限制 ${formatSize(props.maxSize)}`)
    return false
  }
  return true
}
function onChange(file, list) { fileList.value = list }
function onRemove(file, list) {
  fileList.value = list
  emit('remove', file)
}
function clearAll() {
  uploadRef.value && uploadRef.value.clearFiles()
  fileList.value = []
  progress.value = 0
}

async function startUpload() {
  if (!fileList.value.length) return
  uploading.value = true
  progress.value = 0
  try {
    for (const f of fileList.value) {
      const data = await uploadFile(f.raw || f, (p) => { progress.value = Math.min(99, Math.floor(p || 0)) })
      progress.value = 100
      emit('success', data, f)
    }
    ElMessage.success(`上传完成（${fileList.value.length} 个文件）`)
    clearAll()
  } catch (err) {
    // request 拦截器统一错误提示
  } finally {
    uploading.value = false
  }
}

defineExpose({ clearAll, startUpload })
</script>

<style scoped>
.text-muted { color: var(--qz-text-secondary); }
.text-primary { color: var(--qz-primary); }
.text-sm { font-size: 13px; }
.mt-8  { margin-top: 8px; }
.mt-12 { margin-top: 12px; }
.mt-16 { margin-top: 16px; }
.p-16  { padding: 16px; }
.gap-8 { gap: 8px; }
.rounded-8 { border-radius: 8px; }
:deep(.el-upload-dragger) { padding: 24px 12px; }
</style>
