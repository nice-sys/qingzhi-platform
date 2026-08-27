<template>
  <div class="qz-page page-my-drafts">
    <div class="flex-between mb-16">
      <div class="text-muted text-sm">
        <el-icon><EditPen /></el-icon>&nbsp;你保存的草稿，可继续编辑后提交，或删除。
      </div>
      <el-button type="primary" @click="$router.push('/resource/publish')">
        <el-icon><Upload /></el-icon>&nbsp;发布新资源
      </el-button>
    </div>

    <EmptyState :total="total" desc="暂无草稿，去发布资源时点击「保存草稿」即可。">
      <template #list>
        <el-table :data="list" stripe>
          <el-table-column label="标题" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">
              <span :class="!row.title ? 'text-muted' : ''">
                {{ row.title || '（未命名草稿）' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="分类" width="120">
            <template #default="{ row }">
              <span :class="!row.course ? 'text-muted' : ''">
                {{ row.course || '—' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="文件" width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <span :class="!row.fileName ? 'text-muted' : ''">
                {{ row.fileName || '（未上传）' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="最后保存" width="170">
            <template #default="{ row }">{{ formatTime(row.updatedAt || row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="goEdit(row)">继续编辑</el-button>
              <el-popconfirm title="确认删除该草稿？删除后不可恢复。" @confirm="del(row)">
                <template #reference>
                  <el-button link type="danger">删除草稿</el-button>
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Pagination from '@/components/common/Pagination.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { listMyDrafts, deleteDraft } from '@/api/resource'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const formatTime = formatDateTime

async function fetch() {
  try {
    const d = await listMyDrafts({ ...query })
    list.value  = d.list || []
    total.value = d.total || 0
  } catch (err) {
    const msg = err?.message || '加载草稿列表失败'
    ElMessage.error(msg)
  }
}
async function del(r) {
  try {
    await deleteDraft(r.id)
    ElMessage.success('草稿已删除')
    fetch()
  } catch (err) {
    const msg = err?.message || '删除草稿失败'
    ElMessage.error(msg)
  }
}
function goEdit(row) {
  router.push({ path: '/resource/publish', query: { draftId: row.id } })
}
onMounted(fetch)
</script>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.text-muted { color: var(--qz-text-secondary); }
.text-sm { font-size: 13px; }
</style>
