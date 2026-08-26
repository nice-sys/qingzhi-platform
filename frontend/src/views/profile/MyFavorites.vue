<template>
  <div class="qz-page page-my-favorites">
    <div class="text-muted text-sm mb-16">
      <el-icon><Star /></el-icon>&nbsp;一键收藏的资源列表，点击详情快速访问。
    </div>

    <EmptyState :total="total" desc="暂无收藏，去资源中心看看吧！" icon="Star">
      <template #list>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="r in list" :key="r.resourceId || r.id" class="mb-16">
            <ResourceCard
              :data="normalize(r)"
              :favorited="true"
              @click="goDetail(r)"
              @toggle-favorite="toggleFav"
            />
          </el-col>
        </el-row>
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
import EmptyState from '@/components/common/EmptyState.vue'
import ResourceCard from '@/components/resource/ResourceCard.vue'
import Pagination from '@/components/common/Pagination.vue'
import { listMyFavorites, removeFavorite } from '@/api/favorite'

const router = useRouter()
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 12 })

function normalize(item) {
  // FavoriteListResponse 每条是 { id, resourceId, createdAt, resource?: ResourceDetail... }
  if (item.resource) return { ...item.resource, favId: item.id }
  return { ...item, id: item.resourceId || item.id }
}
function goDetail(r) {
  const rid = r.resourceId || r.id
  router.push(`/resource/${rid}`)
}
async function toggleFav(r) {
  try {
    await removeFavorite(r.resourceId || r.id)
    ElMessage.success('已取消收藏')
    fetch()
  } catch (_) {}
}
async function fetch() {
  try {
    const d = await listMyFavorites({ ...query })
    list.value  = d.list || []
    total.value = d.total || 0
  } catch (_) {}
}
onMounted(fetch)
</script>

<style scoped>
.text-muted { color: var(--qz-text-secondary); }
.text-sm { font-size: 13px; }
.mb-16 { margin-bottom: 16px; }
</style>
