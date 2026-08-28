<template>
  <div class="qz-page page-resource-publish">
    <el-card class="qz-card" shadow="never">
      <template #header>
        <div class="flex-between">
          <div>
            <strong>{{ mode === 'update' ? '📝 编辑资源' : '📤 发布新资源' }}</strong>
            <el-tag v-if="hasDraft" type="info" size="small" effect="plain" class="ml-8">
              已恢复草稿（{{ draftSavedAt }}）
            </el-tag>
          </div>
          <div class="flex gap-8 items-center">
            <el-button link type="primary" :disabled="!form.title && !form.description" @click="saveDraft">
              <el-icon><DocumentAdd /></el-icon>&nbsp;{{ hasDraft ? '更新草稿' : '保存草稿' }}
            </el-button>
            <el-button link type="primary" @click="$router.back()">
              <el-icon><ArrowLeft /></el-icon>&nbsp;返回
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        title="文件说明：请先上传主文件（相同文件哈希自动命中秒传，秒传不上传真实字节）。补充描述后提交审核，管理员通过后即可公开下载。"
        type="info"
        :closable="false"
        show-icon
        class="mb-16"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" size="default">
        <el-form-item label="文件" prop="fileStorageId" required>
          <FileUpload
            :limit="1"
            :max-size="MAX_UPLOAD_SIZE"
            @success="onFileOk"
            @remove="onFileRm"
          />
          <div v-if="fileInfo.name" class="mt-12 text-muted text-sm">
            已选文件：
            <el-icon class="align-middle mr-4"><component :is="fileInfo.icon" /></el-icon>
            <b class="text-primary">{{ fileInfo.name }}</b>
            （{{ formatSize(fileInfo.size) }} · {{ fileInfo.typeLabel }}）
            <el-tag v-if="fileInfo.quick" type="success" size="small" effect="plain" class="ml-8">命中秒传 ✓</el-tag>
            <el-tag v-else type="info" size="small" effect="plain" class="ml-8">新上传</el-tag>
            <el-button link type="danger" class="ml-8" @click="onFileRm">移除文件</el-button>
          </div>
          <div v-else class="mt-4 text-muted text-sm">
            ⚠️ 文件上传后会被系统自动识别类型。最大单文件 {{ formatSize(MAX_UPLOAD_SIZE) }}
          </div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="16">
            <el-form-item label="资源标题" prop="title">
              <el-input
                v-model="form.title"
                maxlength="200"
                placeholder="准确描述资源内容，便于检索；例如：计算机网络期末复习题2024版"
              >
                <template #suffix><span class="counter">{{ titleCount }}/200</span></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="课程分类" prop="course">
              <el-select v-model="form.course" class="w-full" placeholder="选择课程分类" filterable>
                <el-option
                  v-for="c in CATEGORY_OPTIONS"
                  :key="c.value"
                  :label="c.label"
                  :value="c.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="资源描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="6"
            maxlength="1000"
            placeholder="简要介绍：适用年级/课程、内容结构、章节要点、使用建议等（10-1000字）"
          />
          <div class="text-right text-muted text-sm mt-4">
            当前：<b class="text-primary">{{ descCount }}</b>/1000 字
            &nbsp;|&nbsp;
            <span :class="descCount >= 10 ? 'text-success' : 'text-danger'">{{ descCount >= 10 ? '✓ 已达到最短字数要求' : `还差 ${10 - descCount} 字` }}</span>
          </div>
        </el-form-item>

        <el-form-item label="预览摘要">
          <el-card class="preview-card" shadow="never" :body-style="{ padding: '12px 16px' }">
            <div v-if="!form.title && !form.description" class="text-muted text-sm">
              内容会随你填写实时显示在这里~
            </div>
            <template v-else>
              <div class="flex-between mb-8">
                <h4 class="mb-0">{{ form.title || '（未填写标题）' }}</h4>
                <div>
                  <el-tag size="small" effect="plain" v-if="form.course">{{ form.course }}</el-tag>
                  <el-tag size="small" effect="plain" type="info" class="ml-4" v-if="fileInfo.typeLabel">{{ fileInfo.typeLabel }}</el-tag>
                </div>
              </div>
              <p class="mb-0 text-muted whitespace-pre-wrap" style="line-height:1.9">
                {{ form.description || '（暂无描述）' }}
              </p>
              <div v-if="tags.length" class="mt-8">
                <el-tag
                  v-for="t in tags"
                  :key="t"
                  size="small"
                  effect="plain"
                  class="mr-4 mb-4"
                  type="success"
                >#{{ t }}</el-tag>
              </div>
            </template>
          </el-card>
        </el-form-item>

        <el-form-item label="关键词标签">
          <el-select
            v-model="tags"
            filterable
            allow-create
            multiple
            default-first-option
            placeholder="回车新增，最多 5 个标签"
            :reserve-keyword="false"
            style="width:100%"
            @change="onTagsChange"
          >
            <el-option
              v-for="t in RESOURCE_TAG_PRESETS"
              :key="t"
              :label="t"
              :value="t"
            />
          </el-select>
          <div class="text-muted text-sm mt-4 flex-between">
            <span>💡 已选 {{ tags.length }}/{{ MAX_TAGS }}；可使用预设，也可自创新标签</span>
            <el-button
              link
              type="danger"
              size="small"
              :disabled="!tags.length"
              @click="tags = []; onTagsChange(tags)"
            >清空全部</el-button>
          </div>
        </el-form-item>

        <div class="text-right pt-8">
          <el-button @click="discardDraft">丢弃草稿并返回</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">
            {{ mode === 'update' ? '保存修改（重新进入审核）' : '提交发布（进入审核）' }}
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import FileUpload from '@/components/common/FileUpload.vue'
import {
  publishResource, updateResource, resourceDetail,
  saveDraftResource, getDraft, deleteDraft
} from '@/api/resource'
import { formatFileSize } from '@/utils/format'
import {
  CATEGORY_OPTIONS, RESOURCE_TAG_PRESETS, MAX_TAGS, MAX_UPLOAD_SIZE,
  resolveTypeByFilename, RESOURCE_TYPE_LABEL, TYPE_ICON
} from '@/utils/constants'

const DRAFT_KEY = 'qz_resource_draft_v1'
const DRAFT_TTL = 30 * 60 * 1000 // 30 分钟（localStorage 辅助兜底）

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const tags = ref([])
const hasDraft = ref(false)
const draftSavedAt = ref('')
const draftId = ref(0) // 当前关联的后端草稿ID（0 = 尚未保存为后端草稿）

const mode = computed(() => (route.params.id ? 'update' : 'publish'))
const editingId = computed(() => Number(route.params.id) || 0)

const form = reactive({
  fileStorageId: 0,
  fileName: '',
  filePath: '',
  fileSize: 0,
  fileExt: '',
  fileHash: '',
  title: '',
  course: '',
  description: '',
  tags: ''
})
const fileInfo = reactive({
  name: '', size: 0, quick: false,
  type: '', typeLabel: '', icon: TYPE_ICON.other
})

const rules = {
  fileStorageId: [{ required: true, message: '请先上传文件', trigger: ['change', 'submit'] }],
  title:         [{ required: true, message: '请填写资源标题', trigger: ['blur', 'change', 'submit'] },
                  { min: 5, max: 200, message: '5-200 字符', trigger: ['blur', 'change', 'submit'] }],
  course:        [{ required: true, message: '请选择课程分类', trigger: ['change', 'submit'] }],
  description:   [{ required: true, message: '请填写资源描述', trigger: ['blur', 'change', 'submit'] },
                  { min: 10, max: 1000, message: '10-1000 字符', trigger: ['blur', 'change', 'submit'] }]
}
const formatSize = formatFileSize

const titleCount = computed(() => (form.title || '').length)
const descCount  = computed(() => (form.description || '').length)

function onTagsChange(list) {
  const arr = (list || []).slice(0, MAX_TAGS)
  tags.value = arr
  form.tags = arr.join(',')
  saveDraft(true) // 静默保存
}
function onFileOk(data) {
  if (!data) return
  const sid = Number(data.fileStorageId || data.storageId || data.id || 0)
  form.fileStorageId = sid > 0 ? sid : 0
  // ⚠️ 关键：把 uploadFile 返回的 6 个文件字段同步写入 form
  // 这样后端 publishResource 无需走 fileStorageId 回填分支，避免 DB 无 original_file_name 列的限制
  form.fileName = data.fileName || data.originalName || data.name || ''
  form.filePath = data.filePath || data.storagePath || data.path || ''
  form.fileSize = Number(data.fileSize || 0)
  form.fileExt  = data.fileExt || data.ext || parseFileExt(form.fileName)
  form.fileHash = data.fileHash || ''
  // 显示用 fileInfo
  fileInfo.name = form.fileName
  fileInfo.size = form.fileSize
  fileInfo.quick = !!data.quickUpload || !!data.hitQuickUpload
  const type = data.type || resolveTypeByFilename(form.fileName)
  fileInfo.type = type
  fileInfo.typeLabel = RESOURCE_TYPE_LABEL[type] || RESOURCE_TYPE_LABEL.other
  fileInfo.icon = TYPE_ICON[type] || TYPE_ICON.other
  // 没拿到合法 fileStorageId 直接报提示，避免用户继续填完标题后才发现没上传成功
  if (form.fileStorageId <= 0) {
    ElMessage.warning('文件上传返回数据异常，未关联到文件，请重新选择文件上传')
  }
  saveDraft(true)
}
function parseFileExt(name) {
  if (!name || !name.includes('.')) return ''
  return name.split('.').pop().toLowerCase()
}
function onFileRm() {
  form.fileStorageId = 0
  form.fileName = ''
  form.filePath = ''
  form.fileSize = 0
  form.fileExt = ''
  form.fileHash = ''
  fileInfo.name = ''
  fileInfo.size = 0
  fileInfo.quick = false
  fileInfo.type = ''
  fileInfo.typeLabel = ''
  fileInfo.icon = TYPE_ICON.other
  saveDraft(true)
}

/* -------------------- 草稿系统（后端持久化 + localStorage 兜底 30min） -------------------- */
function nowStr() {
  const d = new Date()
  const pad = (n) => n.toString().padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
let saveDraftPromise = null
async function saveDraft(silent) {
  // localStorage 兜底（无论后端是否成功都写本地）
  const payload = {
    savedAt: Date.now(),
    editingId: editingId.value,
    draftId: draftId.value,
    form: { ...form },
    fileInfo: { ...fileInfo },
    tags: [...tags.value]
  }
  try { localStorage.setItem(DRAFT_KEY, JSON.stringify(payload)) } catch (_) {}

  // 保存后端草稿（编辑 update 模式下不调用草稿接口，草稿仅用于 publish 模式）
  if (mode.value === 'update') {
    if (!silent) ElMessage.success('已记录本地修改')
    return
  }

  // 防抖合并：如果上一次还在执行，复用 Promise
  if (saveDraftPromise) return saveDraftPromise
  const body = { ...form }
  if (draftId.value > 0) body.id = draftId.value
  saveDraftPromise = (async () => {
    try {
      const d = await saveDraftResource(body)
      const newId = Number(d?.draftId) || 0
      if (newId > 0) draftId.value = newId
      hasDraft.value = true
      draftSavedAt.value = nowStr()
      if (!silent) ElMessage.success('草稿已保存')
    } catch (err) {
      if (!silent) {
        const msg = err?.message || '草稿保存失败，已本地缓存'
        ElMessage.warning(msg)
      }
    } finally {
      saveDraftPromise = null
    }
  })()
  return saveDraftPromise
}
function loadLocalDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) return false
    const p = JSON.parse(raw)
    if (!p || Date.now() - (p.savedAt || 0) > DRAFT_TTL) {
      localStorage.removeItem(DRAFT_KEY); return false
    }
    if ((p.editingId || 0) !== editingId.value) return false
    Object.assign(form, p.form || {})
    Object.assign(fileInfo, p.fileInfo || {})
    tags.value = p.tags || []
    if (p.draftId > 0) draftId.value = Number(p.draftId) || 0
    hasDraft.value = true
    draftSavedAt.value = new Date(p.savedAt).toLocaleTimeString('zh-CN', { hour12: false })
    return true
  } catch (_) { return false }
}
async function loadServerDraft(id) {
  if (!id) return false
  try {
    const d = await getDraft(id)
    form.title = d.title || ''
    form.course = d.course || ''
    form.description = d.description || ''
    form.tags = d.tags || ''
    form.fileStorageId = Number(d.fileStorageId) || 0
    // ⚠️ 回填 6 个文件字段到 form（与 onFileOk 一致）
    form.fileName = d.fileName || ''
    form.filePath = d.filePath || ''
    form.fileSize = Number(d.fileSize || 0)
    form.fileExt  = d.fileExt  || parseFileExt(form.fileName)
    form.fileHash = d.fileHash || ''
    tags.value = (form.tags || '').split(',').filter(Boolean).slice(0, MAX_TAGS)
    if (form.fileStorageId > 0 || form.fileName) {
      fileInfo.name = form.fileName
      fileInfo.size = form.fileSize
      const t = resolveTypeByFilename(form.fileName)
      fileInfo.type = t
      fileInfo.typeLabel = RESOURCE_TYPE_LABEL[t] || RESOURCE_TYPE_LABEL.other
      fileInfo.icon = TYPE_ICON[t] || TYPE_ICON.other
    }
    draftId.value = id
    hasDraft.value = true
    draftSavedAt.value = d.updatedAt ? new Date(d.updatedAt).toLocaleTimeString('zh-CN', { hour12: false }) : nowStr()
    return true
  } catch (err) {
    const msg = err?.message || '加载草稿失败'
    ElMessage.warning(msg)
    return false
  }
}
function clearDraft() {
  try { localStorage.removeItem(DRAFT_KEY) } catch (_) {}
}
async function discardDraft() {
  const hasContent = titleCount.value > 0 || descCount.value > 0 || form.fileStorageId > 0 || draftId.value > 0
  if (hasContent) {
    try {
      await ElMessageBox.confirm('当前输入内容将被丢弃，确定返回吗？', '提示', { type: 'warning' })
    } catch (_) { return }
  }
  clearDraft()
  router.back()
}

/* -------------------- 提交 -------------------- */
async function submit() {
  try {
    await formRef.value.validate()
  } catch (err) {
    const firstErr = Array.isArray(err) ? err[0]?.message : (err?.message || '请完善表单必填项')
    ElMessage.warning(`请完善表单：${firstErr}`)
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    if (mode.value === 'update') {
      await updateResource(editingId.value, { ...form })
      ElMessage.success('修改成功，进入待审核队列')
      clearDraft()
      router.replace(`/resource/${editingId.value}`)
    } else {
      await publishResource({ ...form })
      ElMessage.success('发布成功，等待管理员审核')
      // 发布成功后，不再主动删除后端草稿（避免触发 releaseReference 引用计数归零误删磁盘文件）
      // - 草稿保留在「我的草稿」页面，用户可自行手动删除或再次基于草稿发布
      // - 仅清除 localStorage 本地草稿（form 输入内容的临时保存）
      clearDraft()
      router.replace('/profile/resources')
    }
  } catch (err) {
    const msg = err?.message || err?.msg || (err && typeof err === 'string' ? err : '资源发布失败，请检查网络或稍后重试')
    ElMessage.error(msg)
  } finally { submitting.value = false }
}

/* -------------------- 编辑模式 / 草稿回填 -------------------- */
async function loadEditing() {
  // 1. 如果 URL 里有 draftId，优先从后端加载草稿（发布页继续编辑草稿场景）
  const qid = Number(route.query?.draftId) || 0
  if (qid > 0 && mode.value === 'publish') {
    const ok = await loadServerDraft(qid)
    if (ok) return
  }
  // 2. 编辑 update 模式：读后端资源详情（并优先尝试从 localStorage 草稿恢复未保存改动）
  if (!editingId.value) return
  const localOk = loadLocalDraft()
  if (localOk) return
  try {
    const d = await resourceDetail(editingId.value)
    form.title = d.title || ''
    form.course = d.course || ''
    form.description = d.description || ''
    form.tags = d.tags || ''
    tags.value = (form.tags || '').split(',').filter(Boolean).slice(0, MAX_TAGS)
    const fsId = Number(d.fileStorageId || d.storageId || d.id || 0)
    // ⚠️ 回填 6 个文件字段到 form（与 onFileOk 一致）
    form.fileStorageId = fsId > 0 ? fsId : 0
    form.fileName = d.originalName || d.fileName || ''
    form.filePath = d.filePath || d.storagePath || ''
    form.fileSize = Number(d.fileSize || 0)
    form.fileExt  = d.fileExt  || parseFileExt(form.fileName)
    form.fileHash = d.fileHash || ''
    if (form.fileStorageId > 0 || form.fileName) {
      fileInfo.name = form.fileName
      fileInfo.size = form.fileSize
      const t = d.type || resolveTypeByFilename(form.fileName)
      fileInfo.type = t
      fileInfo.typeLabel = RESOURCE_TYPE_LABEL[t] || RESOURCE_TYPE_LABEL.other
      fileInfo.icon = TYPE_ICON[t] || TYPE_ICON.other
    }
  } catch (_) {}
}

/* 自动草稿（防抖 1s）：至少填了一个字段才保存（避免空草稿一堆） */
let timer = null
watch([() => form.title, () => form.description, () => form.course], () => {
  if (timer) clearTimeout(timer)
  const hasAny = titleCount.value > 0 || descCount.value > 0 || form.course || form.fileStorageId > 0
  if (!hasAny) return
  timer = setTimeout(() => saveDraft(true), 1000)
})
onBeforeUnmount(() => { if (timer) clearTimeout(timer) })

onMounted(loadEditing)
defineExpose({ saveDraft, clearDraft })
</script>

<style scoped>
.text-muted { color: var(--qz-text-secondary); }
.text-primary{ color: var(--qz-primary); }
.text-success{ color: var(--el-color-success); }
.text-danger { color: var(--el-color-danger); }
.text-sm{ font-size: 13px; }
.text-right { text-align: right; }
.mb-0  { margin-bottom: 0; }
.mb-4  { margin-bottom: 4px; }
.mb-8  { margin-bottom: 8px; }
.mt-4  { margin-top: 4px; }
.mt-8  { margin-top: 8px; }
.mt-12 { margin-top: 12px; }
.mt-16 { margin-top: 16px; }
.ml-4  { margin-left: 4px; }
.ml-8  { margin-left: 8px; }
.mr-4  { margin-right: 4px; }
.pt-8  { padding-top: 8px; }
.gap-8 { gap: 8px; }
.align-middle { vertical-align: middle; }
.w-full { width: 100%; }
.counter {
  font-size: 12px;
  color: var(--qz-text-secondary);
}
.preview-card {
  background: linear-gradient(180deg, #fbfcff 0%, #f4f8f7 100%);
  border: 1px dashed var(--qz-border-light);
}
.whitespace-pre-wrap { white-space: pre-wrap; }
</style>
