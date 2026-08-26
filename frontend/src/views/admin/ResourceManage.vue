<template>
  <div class="qz-page page-resource-manage" v-permission="'admin'">
    <el-card class="qz-card mb-16" shadow="never">
      <el-form :model="query" inline size="default" label-position="right">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="标题/上传者/描述" clearable style="width:240px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.reviewStatus" clearable style="width:120px" placeholder="全部">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.category" clearable style="width:140px" placeholder="课程分类">
            <el-option
              v-for="c in CATEGORY_OPTIONS"
              :key="c.value"
              :label="c.label"
              :value="c.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetch">
            <el-icon><Search /></el-icon>&nbsp;搜索
          </el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 批量操作工具栏 -->
    <div v-if="selected.length" class="qz-card mb-12 p-12 batch-toolbar">
      <div class="flex flex-wrap gap-8 items-center">
        <el-tag effect="plain" type="danger" size="small">批量操作</el-tag>
        <span class="text-muted text-sm">已选 <b class="text-primary">{{ selected.length }}</b> 条</span>
        <el-button
          type="danger"
          size="small"
          @click="batchDelete"
        >
          <el-icon><Delete /></el-icon>&nbsp;批量删除（{{ selected.length }}）
        </el-button>
        <el-button link type="primary" size="small" @click="clearSelection">清空选择</el-button>
      </div>
    </div>

    <el-card class="qz-card" shadow="never">
      <EmptyState :total="total" desc="暂无资源">
        <template #list>
          <el-table
            :data="list"
            stripe
            @selection-change="onSelectionChange"
            ref="tableRef"
          >
            <el-table-column type="selection" width="52" align="center" />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }"><StatusTag :status="row.reviewStatus" /></template>
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
            <el-table-column prop="downloadCount" label="下载" width="80" align="center" />
            <el-table-column prop="favoriteCount" label="收藏" width="80" align="center" />
            <el-table-column label="更新时间" width="170">
              <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right" align="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  :disabled="Number(row.reviewStatus) !== 1"
                  :title="Number(row.reviewStatus) !== 1 ? '审核通过后可下载' : '下载'"
                  @click="onDownload(row)"
                >下载</el-button>
                <el-button link type="primary" @click="$router.push(`/resource/${row.id}`)">详情</el-button>
                <el-button link type="primary" @click="$router.push(`/resource/${row.id}/update`)">编辑</el-button>
                <el-popconfirm
                  title="删除该资源？（会尝试释放文件存储引用）"
                  @confirm="del(row)"
                >
                  <template #reference>
                    <el-button link type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Delete } from '@element-plus/icons-vue'
import StatusTag  from '@/components/common/StatusTag.vue'
import Pagination from '@/components/common/Pagination.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { listAllResources, adminDeleteResource } from '@/api/admin'
import { downloadResource } from '@/api/resource'
import { CATEGORY_OPTIONS } from '@/utils/constants'
import { ROLE_NAME } from '@/utils/permission'
import { formatDateTime } from '@/utils/format'

const list = ref([])
const total = ref(0)
const selected = ref([])
const tableRef = ref(null)
const query = reactive({ page: 1, size: 10, keyword: '', reviewStatus: '', category: '' })
const formatTime = formatDateTime

function roleTagColor(role) {
  const r = Number(role)
  return r === 0 ? 'var(--qz-role-admin)'
       : r === 1 ? 'var(--qz-role-teacher)'
       : 'var(--qz-role-student)'
}
function roleTagLabel(role) { return ROLE_NAME[Number(role)] || '' }

async function fetch() {
  try {
    const d = await listAllResources({ ...query, reviewStatus: query.reviewStatus === '' ? undefined : query.reviewStatus })
    list.value  = d.list  || []
    total.value = d.total || 0
  } catch (e) {
    ElMessage.warning(e?.message || '获取资源列表失败，请稍后重试')
  }
}
function reset() { query.keyword=''; query.reviewStatus=''; query.category=''; query.page=1; fetch() }
function onSelectionChange(rows) { selected.value = rows }
function clearSelection() { if (tableRef.value) tableRef.value.clearSelection() }

async function onDownload(r) {
  if (Number(r.reviewStatus) !== 1) {
    ElMessage.warning('该资源尚未通过审核，暂不可下载')
    return
  }
  try {
    await downloadResource(r.id, r.originalName || r.fileName || `${r.title}`)
  } catch (e) {
    ElMessage.warning(e?.message || '下载失败，请稍后重试')
  }
}

async function del(r) {
  try {
    await adminDeleteResource(r.id)
    ElMessage.success('已删除')
    fetch()
  } catch (e) {
    ElMessage.warning(e?.message || '删除失败，请稍后重试')
  }
}

async function batchDelete() {
  const rows = selected.value
  if (!rows.length) return
  try {
    await ElMessageBox.confirm(
      `确定要批量删除选中的 ${rows.length} 条资源？\n此操作会同时释放文件存储引用，且不可恢复。`,
      '批量删除',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch (e) { return }
  let ok = 0, fail = 0
  for (const r of rows) {
    try { await adminDeleteResource(r.id); ok++ }
    catch { fail++ }
  }
  if (ok) ElMessage.success(`已成功删除 ${ok} 条${fail ? `，失败 ${fail} 条` : ''}`)
  else ElMessage.warning(`批量删除失败，请检查后端接口`)
  clearSelection()
  fetch()
}

onMounted(fetch)
</script>

<style scoped>
.mb-12 { margin-bottom: 12px; }
.mb-16 { margin-bottom: 16px; }
.mt-2  { margin-top: 2px; }
.gap-6 { gap: 6px; }
.gap-8 { gap: 8px; }
.p-12  { padding: 12px; }
.ml-8  { margin-left: 8px; }
.text-muted { color: var(--qz-text-secondary); }
.text-primary { color: var(--qz-primary); }
.text-sm { font-size: 13px; }
.text-xs { font-size: 12px; }
.flex { display: flex; }
.flex-wrap { flex-wrap: wrap; }
.items-center { align-items: center; }
.batch-toolbar :deep(.el-card__body) { padding: 12px; }
</style>
