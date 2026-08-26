<template>
  <div class="qz-page page-excel-import" v-permission="'admin'">
    <el-row :gutter="16">
      <el-col :span="14">
        <el-card class="qz-card" shadow="never">
          <template #header>
            <div class="flex-between">
              <strong>📥 批量导入用户（Excel / CSV）</strong>
              <div>
                <el-button link type="primary" @click="downloadTemplate">
                  <el-icon><Download /></el-icon>&nbsp;下载 CSV 模板
                </el-button>
              </div>
            </div>
          </template>

          <el-alert
            type="info"
            show-icon
            :closable="false"
            class="mb-16"
          >
            <template #title>模板格式：第 1 行为表头，列顺序固定（与模板保持一致）</template>
            <template #default>
              <ol style="margin:4px 0 0; padding-left:22px; line-height:1.9;">
                <li v-for="(c, i) in EXCEL_TEMPLATE_COLUMNS" :key="c.key">
                  第 {{ i + 1 }} 列 - <b>{{ c.label }}</b>
                  <span v-if="c.required" class="text-danger">（必填）</span>
                  ，示例：<code>{{ c.example }}</code>
                </li>
              </ol>
              <div class="mt-8 text-muted">
                角色值：1=教师 2=学生；<b>角色 0=管理员 禁止通过导入新增</b>，否则该整行会被后端拒绝。
              </div>
            </template>
          </el-alert>

          <el-upload
            ref="uploadRef"
            drag
            action=""
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls,.csv"
            :file-list="fileList"
            :on-change="(f,l) => fileList = l"
            :before-upload="beforeUpload"
          >
            <el-icon class="el-icon--upload" :size="40"><UploadFilled /></el-icon>
            <div class="el-upload__text mt-8">
              将 Excel（.xlsx / .xls）或 CSV 文件拖到此处，或<em class="text-primary">点击选择</em>
            </div>
            <template #tip>
              <div class="el-upload__tip text-muted text-sm mt-8">
                支持 .xlsx / .xls / .csv，单文件 <= {{ formatSize(EXCEL_IMPORT_MAX) }}
              </div>
            </template>
          </el-upload>

          <div v-if="currentFile" class="mt-12 text-muted text-sm">
            已选文件：<b class="text-primary">{{ currentFile.name }}</b>（{{ formatSize(currentFile.size) }}）
          </div>

          <div class="mt-16 flex gap-8 items-center">
            <el-button type="primary" :disabled="!fileList.length || loading" :loading="loading" @click="doImport">
              <el-icon><Upload /></el-icon>&nbsp;开始导入
            </el-button>
            <el-button :disabled="!fileList.length && !result" @click="clear">
              清空列表
            </el-button>
            <div class="flex-1" />
            <el-button
              v-if="result && (result.errors || []).length"
              type="warning"
              @click="exportErrorRows"
            >
              <el-icon><Download /></el-icon>&nbsp;导出失败行 CSV
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <ImportResult
          v-if="result"
          :result="result"
          @close="result = null"
        >
          <template #actions>
            <el-button
              v-if="(result.errors || []).length"
              type="warning"
              @click="exportErrorRows"
            >
              <el-icon><Download /></el-icon>&nbsp;导出失败行
            </el-button>
            <el-button @click="result = null">关闭结果</el-button>
          </template>
        </ImportResult>

        <el-card v-else class="qz-card text-muted text-sm" shadow="never">
          <template #header><strong>💡 使用说明</strong></template>
          <ol style="padding-left:20px; line-height:2; margin:0;">
            <li>点击右上角 <b>「下载 CSV 模板」</b>，严格按列顺序填写（可在 Excel 中打开再另存为 .xlsx 导入）。</li>
            <li>密码规则：>= 8 位，必须包含 <b>字母</b> + <b>数字</b>（不合法的行会被拒绝）。</li>
            <li>账号唯一性：数据库中已存在的账号会报错跳过（不会覆盖已有用户）。</li>
            <li>角色 0 管理员不允许通过 Excel 新增，写了 0 的行会被后端直接拒绝。</li>
            <li>导入完成后，如有失败行：点 <b>「导出失败行 CSV」</b>，修正后只导入失败行即可。</li>
          </ol>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ImportResult from '@/components/admin/ImportResult.vue'
import { importUsers } from '@/api/admin'
import { EXCEL_TEMPLATE_COLUMNS, EXCEL_IMPORT_MAX } from '@/utils/constants'
import { formatFileSize } from '@/utils/format'

const uploadRef = ref(null)
const fileList  = ref([])
const loading   = ref(false)
const result    = ref(null)

const formatSize = formatFileSize
const currentFile = computed(() => (fileList.value && fileList.value[0] ? (fileList.value[0].raw || fileList.value[0]) : null))

/* -------------------- 文件校验 -------------------- */
function beforeUpload(f) {
  if (f.size > EXCEL_IMPORT_MAX) {
    ElMessage.error(`文件超过 ${formatSize(EXCEL_IMPORT_MAX)} 限制`)
    return false
  }
  const name = (f.name || '').toLowerCase()
  if (!name.endsWith('.xlsx') && !name.endsWith('.xls') && !name.endsWith('.csv')) {
    ElMessage.error('仅支持 .xlsx / .xls / .csv 格式')
    return false
  }
  return true
}
function clear() {
  uploadRef.value && uploadRef.value.clearFiles()
  fileList.value = []
  result.value = null
}

/* -------------------- 导入 -------------------- */
async function doImport() {
  if (!fileList.value.length) return
  loading.value = true
  try {
    const d = await importUsers(currentFile.value, () => {})
    result.value = d
    const s = d && d.successCount !== undefined ? d.successCount : (d && d.success || 0)
    const f = d && d.failCount !== undefined ? d.failCount : (d && d.fail || 0)
    ElMessage.success(`导入完成：成功 ${s}，失败 ${f}`)
  } catch (_) {} finally { loading.value = false }
}

/* -------------------- CSV 工具：UTF-8 BOM + 安全字段转义 -------------------- */
function csvEscape(v) {
  if (v === null || v === undefined) return ''
  const s = String(v)
  if (/[",\r\n]/.test(s)) return '"' + s.replace(/"/g, '""') + '"'
  return s
}
function buildCsv(rows, headers) {
  const headerLine = (headers || []).map(csvEscape).join(',')
  const body = (rows || []).map(r => r.map(csvEscape).join(',')).join('\r\n')
  return '\uFEFF' + headerLine + '\r\n' + body + '\r\n'
}
function triggerDownloadBlob(filename, mime, content) {
  const blob = new Blob([content], { type: mime })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 2000)
}

/* -------------------- 下载模板 CSV -------------------- */
function downloadTemplate() {
  const headers = EXCEL_TEMPLATE_COLUMNS.map(c => c.label + (c.required ? '（必填）' : ''))
  const sample  = EXCEL_TEMPLATE_COLUMNS.map(c => c.example || '')
  const csv = buildCsv([sample], headers)
  triggerDownloadBlob(
    `青知共享_批量导入用户模板_${yyyyMMdd()}.csv`,
    'text/csv;charset=utf-8',
    csv
  )
  ElMessage.success('模板已下载，可在 Excel 中编辑后再导入')
}

/* -------------------- 导出失败行 -------------------- */
function exportErrorRows() {
  const errs = (result.value && result.value.errors) || []
  if (!errs.length) { ElMessage.warning('没有失败行'); return }
  const headers = EXCEL_TEMPLATE_COLUMNS.map(c => c.label).concat(['失败原因'])
  // 后端 errors: [{row,key,message, rowData?}]
  const rows = errs.map(e => {
    const raw = e.rowData || {}
    return EXCEL_TEMPLATE_COLUMNS
      .map(c => raw[c.key] !== undefined ? raw[c.key] : '')
      .concat([e.message || ''])
  })
  const csv = buildCsv(rows, headers)
  triggerDownloadBlob(
    `青知共享_导入失败行_${yyyyMMdd()}.csv`,
    'text/csv;charset=utf-8',
    csv
  )
  ElMessage.success(`失败行（${errs.length} 条）已导出为 CSV`)
}
function yyyyMMdd() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}${pad(d.getMonth()+1)}${pad(d.getDate())}_${pad(d.getHours())}${pad(d.getMinutes())}`
}
</script>

<style scoped>
.text-muted { color: var(--qz-text-secondary); }
.text-primary { color: var(--qz-primary); }
.text-danger { color: var(--el-color-danger); }
.text-sm{ font-size: 13px; }
.mb-16 { margin-bottom: 16px; }
.mt-4  { margin-top: 4px; }
.mt-8  { margin-top: 8px; }
.mt-12 { margin-top: 12px; }
.mt-16 { margin-top: 16px; }
.ml-8  { margin-left: 8px; }
.gap-8 { gap: 8px; }
.flex-1 { flex: 1 1 auto; }
:deep(.el-upload-dragger) { padding: 28px 12px; }
ol code {
  background: #f3f5f7;
  padding: 1px 6px;
  border-radius: 4px;
  color: var(--qz-primary);
}
</style>
