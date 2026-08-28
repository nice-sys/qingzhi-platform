<template>
  <div class="qz-page page-my-resources">
    <div class="flex-between mb-16">
      <div class="text-muted text-sm">
        <el-icon><FolderOpened /></el-icon>&nbsp;你发布的全部资源，可随时编辑或删除。
      </div>
      <el-button type="primary" @click="$router.push('/resource/publish')">
        <el-icon><Upload /></el-icon>&nbsp;发布新资源
      </el-button>
    </div>

    <EmptyState :total="total" desc="你还没有发布资源，去发布第一个吧！">
      <template #list>
        <el-table :data="list" stripe>
          <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
          <el-table-column label="分类" width="120">
            <template #default="{ row }">
              <span :class="!row.course ? 'text-muted' : ''">{{ row.course || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ formatFileExt(row.fileExt, row.fileName) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <StatusTag :status="row.reviewStatus" kind="review" />
            </template>
          </el-table-column>
          <el-table-column prop="downloadCount" label="下载" width="90" />
          <el-table-column prop="favoriteCount" label="收藏" width="80" />
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">{{ formatTime(row.updateTime || row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="$router.push(`/resource/${row.id}`)">详情</el-button>
              <el-button link type="primary" @click="$router.push(`/resource/${row.id}/update`)">编辑</el-button>
              <el-popconfirm title="确认删除该资源？" @confirm="del(row)">
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag  from '@/components/common/StatusTag.vue'
import Pagination from '@/components/common/Pagination.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { listMyResources, deleteResource } from '@/api/resource'
import { formatDateTime } from '@/utils/format'

const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const formatTime = formatDateTime

function formatFileExt(ext, name) {
  if (ext && ext.trim()) return ext.trim().toUpperCase()
  if (name && name.includes('.')) return name.split('.').pop().toUpperCase()
  return '—'
}

async function fetch() {
  try {
    const d = await listMyResources({ ...query })
    list.value  = d.list || []
    total.value = d.total || 0
  } catch (_) {}
}
async function del(r) {
  try {
    await deleteResource(r.id)
    ElMessage.success('已删除')
    fetch()
  } catch (_) {}
}
onMounted(fetch)
</script>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.text-muted { color: var(--qz-text-secondary); }
.text-sm { font-size: 13px; }
</style>
