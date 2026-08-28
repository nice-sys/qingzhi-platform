<template>
  <div class="qz-page page-review" v-permission="'admin'">
    <div class="flex-between mb-16">
      <el-tabs v-model="tab" @tab-change="onTab">
        <el-tab-pane label="待审核"  name="pending" />
        <el-tab-pane label="已通过"  name="pass" />
        <el-tab-pane label="已拒绝"  name="reject" />
        <el-tab-pane label="全部"    name="all" />
      </el-tabs>
      <div class="text-muted text-sm">
        共 <b class="text-primary">{{ total }}</b> 条
        <span v-if="selected.length" class="ml-12">
          · 已选 <b class="text-primary">{{ selected.length }}</b> 条
        </span>
      </div>
    </div>

    <!-- 批量操作工具栏 -->
    <div v-if="tab === 'pending'" class="qz-card mb-12 p-12 batch-toolbar">
      <div class="flex flex-wrap gap-8 items-center">
        <el-tag effect="plain" type="warning" size="small">批量操作</el-tag>
        <el-tooltip :content="pendingSelected.length === 0 ? '请先勾选待审核资源' : ''">
          <el-button
            type="success"
            size="small"
            :disabled="pendingSelected.length === 0"
            @click="batchPass"
          >
            <el-icon><Check /></el-icon>&nbsp;批量通过（{{ pendingSelected.length }}）
          </el-button>
        </el-tooltip>
        <el-tooltip :content="pendingSelected.length === 0 ? '请先勾选待审核资源' : ''">
          <el-button
            type="danger"
            size="small"
            :disabled="pendingSelected.length === 0"
            @click="openBatchReject"
          >
            <el-icon><Close /></el-icon>&nbsp;批量拒绝（{{ pendingSelected.length }}）
          </el-button>
        </el-tooltip>
        <el-button link type="primary" size="small" @click="clearSelection" v-if="selected.length">
          清空选择
        </el-button>
      </div>
    </div>

    <el-card class="qz-card" shadow="never">
      <EmptyState :total="total" desc="暂无数据">
        <template #list>
          <el-table
            :data="list"
            stripe
            @selection-change="onSelectionChange"
            ref="tableRef"
          >
            <el-table-column
              v-if="tab === 'pending'"
              type="selection"
              width="52"
              align="center"
              :selectable="(r) => Number(r.reviewStatus) === 0"
            />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
            <el-table-column label="分类" width="120">
              <template #default="{ row }">
                <span :class="!row.course ? 'text-muted' : ''">{{ row.course || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :type="fileExtTagType(row.fileExt)">
                  {{ formatFileExt(row.fileExt, row.fileName) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="上传者" width="220">
              <template #default="{ row }">
                <div class="flex items-center gap-6">
                  <span>{{ row.uploaderName || row.uploaderUsername || '-' }}</span>
                  <el-tag
                    v-if="row.uploaderRole != null"
                    size="small"
                    effect="plain"
                    :color="roleTagColor(row.uploaderRole)"
                  >{{ roleTagLabel(row.uploaderRole) }}</el-tag>
                </div>
                <div class="text-muted text-xs mt-2">ID: {{ row.uploaderId || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }"><StatusTag :status="row.reviewStatus" /></template>
            </el-table-column>
            <el-table-column label="提交时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createTime || row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="view(row)">详情</el-button>
                <template v-if="Number(row.reviewStatus) === 0">
                  <el-button link type="success" @click="pass(row)">通过</el-button>
                  <el-button link type="danger" @click="reject(row)">拒绝</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <Pagination
            :total="total"
            :current="query.page"
            :size="query.size"
            @change="({ page, size }) => { query.page = page; query.size = size; fetch() }"
          />
        </template>
      </EmptyState>
    </el-card>

    <!-- 拒绝弹框 -->
    <el-dialog v-model="rejectVisible" :title="isBatch ? `批量拒绝 ${batchRejectRows.length} 条资源` : '审核拒绝'" width="520">
      <el-form label-width="90px" size="default">
        <el-form-item label="资源" v-if="!isBatch">{{ viewing ? viewing.title : '-' }}</el-form-item>
        <el-form-item label="将处理" v-else>
          <span v-for="(r, i) in batchRejectRows.slice(0, 5)" :key="r.id" class="inline-block mr-8">
            <el-tag size="small" type="info" effect="plain">#{{ r.id }} {{ r.title }}</el-tag>
          </span>
          <span v-if="batchRejectRows.length > 5" class="text-muted text-sm">
            等 {{ batchRejectRows.length }} 条
          </span>
        </el-form-item>
        <el-form-item :label="isBatch ? '统一拒绝原因' : '拒绝原因'">
          <el-input
            v-model="rejectRemark"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="告诉上传者哪里需要修改（将批量应用到所有选中项）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible=false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="doReject">
          {{ isBatch ? '确认批量拒绝' : '确认拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import StatusTag  from '@/components/common/StatusTag.vue'
import Pagination from '@/components/common/Pagination.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { listAllResources, reviewPassResource, reviewRejectResource } from '@/api/admin'
import { REVIEW } from '@/utils/permission'
import { formatDateTime } from '@/utils/format'
import { ROLE_NAME } from '@/utils/permission'

const router = useRouter()
const list = ref([])
const total = ref(0)
const tab = ref('pending')
const viewing = ref(null)
const rejectVisible = ref(false)
const rejectRemark = ref('')
const submitting = ref(false)
const selected = ref([])
const tableRef = ref(null)
const isBatch = ref(false)
const batchRejectRows = ref([])
const query = reactive({ page: 1, size: 10 })
const formatTime = formatDateTime

const statusByTab = computed(() => {
  switch (tab.value) {
    case 'pending': return REVIEW.PENDING
    case 'pass':    return REVIEW.PASS
    case 'reject':  return REVIEW.REJECT
    default:        return ''
  }
})
const pendingSelected = computed(() =>
  selected.value.filter(r => Number(r.reviewStatus) === 0)
)

function roleTagColor(role) {
  const r = Number(role)
  return r === 0 ? 'var(--qz-role-admin)'
       : r === 1 ? 'var(--qz-role-teacher)'
       : 'var(--qz-role-student)'
}
function roleTagLabel(role) { return ROLE_NAME[Number(role)] || '' }

function formatFileExt(ext, name) {
  if (ext && ext.trim()) return ext.trim().toUpperCase()
  if (name && name.includes('.')) return name.split('.').pop().toUpperCase()
  return '未知'
}
function fileExtTagType(ext) {
  const e = (ext || '').toLowerCase()
  if (['pdf'].includes(e)) return 'danger'
  if (['doc', 'docx', 'txt', 'rtf'].includes(e)) return 'primary'
  if (['xls', 'xlsx', 'csv'].includes(e)) return 'success'
  if (['ppt', 'pptx'].includes(e)) return 'warning'
  if (['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'].includes(e)) return 'info'
  if (['zip', 'rar', '7z'].includes(e)) return ''
  return ''
}

async function fetch() {
  try {
    const d = await listAllResources({
      page: query.page, size: query.size,
      reviewStatus: statusByTab.value === '' ? undefined : statusByTab.value
    })
    list.value  = d.list  || []
    total.value = d.total || 0
  } catch (e) {
    ElMessage.warning(e?.message || '获取资源列表失败，请稍后重试')
  }
}
function onTab() { query.page = 1; selected.value = []; fetch() }
function view(r) { router.push(`/resource/${r.id}`) }
function onSelectionChange(rows) { selected.value = rows }
function clearSelection() { if (tableRef.value) tableRef.value.clearSelection() }

async function pass(r) {
  try {
    await ElMessageBox.confirm(`确认通过资源【${r.title}】？`, '提示', { type: 'success' })
    await reviewPassResource(r.id)
    ElMessage.success('已通过')
    fetch()
  } catch (e) {
    if (e !== 'cancel') ElMessage.warning(e?.message || '审核操作失败')
  }
}
function reject(r) {
  isBatch.value = false
  viewing.value = r
  rejectRemark.value = r.reviewRemark || ''
  rejectVisible.value = true
}

async function batchPass() {
  const rows = pendingSelected.value
  if (!rows.length) return
  try {
    await ElMessageBox.confirm(`确定要批量通过选中的 ${rows.length} 条资源？`, '批量审核', { type: 'success' })
  } catch (e) { return }
  let ok = 0, fail = 0
  submitting.value = true
  for (const r of rows) {
    try { await reviewPassResource(r.id); ok++ }
    catch { fail++ }
  }
  submitting.value = false
  if (ok) ElMessage.success(`批量通过 ${ok} 条${fail ? `，失败 ${fail} 条` : ''}`)
  else ElMessage.warning(`批量操作失败，请检查后端接口`)
  clearSelection()
  fetch()
}

function openBatchReject() {
  const rows = pendingSelected.value
  if (!rows.length) return
  isBatch.value = true
  batchRejectRows.value = rows
  viewing.value = null
  rejectRemark.value = '不符合资源规范，请修改后重新提交'
  rejectVisible.value = true
}

async function doReject() {
  submitting.value = true
  try {
    if (isBatch.value) {
      const rows = batchRejectRows.value
      const remark = rejectRemark.value || '不符合资源规范，请修改后重新提交'
      let ok = 0, fail = 0
      for (const r of rows) {
        try {
          await reviewRejectResource({ resourceId: r.id, reviewRemark: remark })
          ok++
        } catch { fail++ }
      }
      rejectVisible.value = false
      if (ok) ElMessage.success(`已批量拒绝 ${ok} 条${fail ? `，失败 ${fail} 条` : ''}`)
      else ElMessage.warning(`批量操作失败，请检查后端接口`)
      clearSelection()
    } else {
      await reviewRejectResource({
        resourceId: viewing.value.id,
        reviewRemark: rejectRemark.value || '不符合资源规范，请修改后重新提交'
      })
      ElMessage.success('已拒绝并通知上传者')
      rejectVisible.value = false
    }
    fetch()
  } catch (e) {
    ElMessage.warning(e?.message || '操作失败，请稍后重试')
  } finally { submitting.value = false }
}

onMounted(fetch)
</script>

<style scoped>
.text-muted { color: var(--qz-text-secondary); }
.text-primary { color: var(--qz-primary); }
.text-sm { font-size: 13px; }
.text-xs { font-size: 12px; }
.mb-12 { margin-bottom: 12px; }
.mb-16 { margin-bottom: 16px; }
.ml-12 { margin-left: 12px; }
.mr-8  { margin-right: 8px; }
.mt-2  { margin-top: 2px; }
.gap-6 { gap: 6px; }
.gap-8 { gap: 8px; }
.p-12  { padding: 12px; }
.flex { display: flex; }
.flex-wrap { flex-wrap: wrap; }
.items-center { align-items: center; }
.inline-block { display: inline-block; }
.batch-toolbar :deep(.el-card__body) { padding: 12px; }
</style>
