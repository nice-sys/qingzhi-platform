<template>
  <div class="qz-page page-resource-list">
    <!-- Filter -->
    <ResourceFilter @query="onFilter" />

    <!-- 高级筛选折叠 -->
    <el-collapse class="adv-collapse mb-12">
      <el-collapse-item title="高级筛选" name="adv">
        <el-form :inline="true" size="default" class="adv-form">
          <el-form-item label="上传者角色">
            <el-select
              v-model="advQuery.uploaderRole"
              placeholder="全部"
              clearable
              style="width:140px"
              @change="applyLocalFilters"
            >
              <el-option
                v-for="o in ROLE_OPTIONS"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="上传时间">
            <el-date-picker
              v-model="advQuery.createdRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width:260px"
              @change="applyLocalFilters"
            />
          </el-form-item>
          <el-form-item label="仅看我收藏">
            <el-switch v-model="advQuery.onlyFav" @change="applyLocalFilters" />
          </el-form-item>
          <el-form-item>
            <el-button link type="primary" @click="resetAdv">重置高级筛选</el-button>
          </el-form-item>
        </el-form>
      </el-collapse-item>
    </el-collapse>

    <!-- 已选筛选条件 Tag 展示 -->
    <div v-if="activeFilterTags.length" class="flex flex-wrap gap-8 items-center mb-12">
      <span class="text-muted text-sm">已筛选：</span>
      <el-tag
        v-for="(t, i) in activeFilterTags"
        :key="i"
        size="small"
        closable
        round
        @close="removeFilterTag(t.key)"
      >{{ t.label }}：{{ t.value }}</el-tag>
      <el-button link type="primary" size="small" @click="clearAllFilters">清空全部</el-button>
    </div>

    <!-- Toolbar -->
    <div class="flex-between mb-12">
      <div class="text-muted text-sm">
        <el-icon><InfoFilled /></el-icon>&nbsp;
        所有资源均经过审核后展示。相同文件仅占用一份存储空间，鼓励重复上传。
        <span v-if="advQuery.onlyFav" class="text-primary ml-8">
          · 已开启「仅看我收藏」({{ displayList.length }}/{{ total }})
        </span>
      </div>
      <div>
        <el-button v-permission="['teacher','student']" type="primary" @click="$router.push('/resource/publish')">
          <el-icon><Upload /></el-icon>&nbsp;发布资源
        </el-button>
      </div>
    </div>

    <!-- Content -->
    <EmptyState :total="displayList.length" desc="暂无符合条件的资源" icon="Search">
      <template #list>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="r in displayList" :key="r.id" class="mb-16">
            <ResourceCard
              :data="r"
              :favorited="favMap[r.id]"
              @click="goDetail(r)"
              @toggle-favorite="toggleFav"
              @quick-download="onQuickDownload"
            />
          </el-col>
        </el-row>
        <Pagination
          :total="total"
          :current="query.page"
          :size="query.size"
          @change="onPage"
        />
      </template>
    </EmptyState>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import ResourceFilter from '@/components/resource/ResourceFilter.vue'
import ResourceCard   from '@/components/resource/ResourceCard.vue'
import Pagination     from '@/components/common/Pagination.vue'
import EmptyState     from '@/components/common/EmptyState.vue'
import { ElMessage } from 'element-plus'
import { InfoFilled, Upload } from '@element-plus/icons-vue'
import { listResource, downloadResource } from '@/api/resource'
import { addFavorite, removeFavorite, listMyFavorites } from '@/api/favorite'
import { ROLE_OPTIONS, CATEGORIES, RESOURCE_TYPE_OPTIONS } from '@/utils/constants'
import { ROLE_NAME } from '@/utils/permission'

const router = useRouter()
const list  = ref([])
const total = ref(0)
const favMap = reactive({})

const query = reactive({
  page: 1, size: 12,
  keyword: '', category: '', type: '', sort: 'newest'
})

const advQuery = reactive({
  uploaderRole: '',
  createdRange: [],
  onlyFav: false
})

const displayList = computed(() => {
  let arr = list.value
  if (advQuery.onlyFav) {
    arr = arr.filter(r => favMap[r.id])
  }
  if (advQuery.uploaderRole !== '' && advQuery.uploaderRole != null) {
    const r = Number(advQuery.uploaderRole)
    arr = arr.filter(x => Number(x.uploaderRole) === r)
  }
  if (advQuery.createdRange && advQuery.createdRange.length === 2) {
    const [s, e] = advQuery.createdRange
    const start = new Date(s + ' 00:00:00').getTime()
    const end   = new Date(e + ' 23:59:59').getTime()
    arr = arr.filter(x => {
      const t = x.createdAt ? new Date(x.createdAt).getTime() : 0
      return t >= start && t <= end
    })
  }
  return arr
})

const activeFilterTags = computed(() => {
  const out = []
  if (query.keyword) out.push({ key: 'keyword', label: '关键词', value: query.keyword })
  if (query.category) out.push({ key: 'category', label: '分类', value: query.category })
  if (query.type) {
    const opt = RESOURCE_TYPE_OPTIONS.find(o => o.value === query.type)
    out.push({ key: 'type', label: '类型', value: opt?.label || query.type })
  }
  if (advQuery.uploaderRole !== '' && advQuery.uploaderRole != null) {
    out.push({ key: 'adv.uploaderRole', label: '上传者角色', value: ROLE_NAME[Number(advQuery.uploaderRole)] || advQuery.uploaderRole })
  }
  if (advQuery.createdRange && advQuery.createdRange.length === 2) {
    out.push({ key: 'adv.createdRange', label: '上传时间', value: advQuery.createdRange.join(' ~ ') })
  }
  if (advQuery.onlyFav) out.push({ key: 'adv.onlyFav', label: '仅看我收藏', value: '已开启' })
  return out
})

function removeFilterTag(key) {
  if (key === 'keyword') query.keyword = ''
  else if (key === 'category') query.category = ''
  else if (key === 'type') query.type = ''
  else if (key === 'adv.uploaderRole') advQuery.uploaderRole = ''
  else if (key === 'adv.createdRange') advQuery.createdRange = []
  else if (key === 'adv.onlyFav') advQuery.onlyFav = false
  if (key.startsWith('adv.')) {
    applyLocalFilters()
  } else {
    fetchList()
  }
}

function clearAllFilters() {
  query.keyword = ''; query.category = ''; query.type = ''
  advQuery.uploaderRole = ''; advQuery.createdRange = []; advQuery.onlyFav = false
  fetchList()
}

async function fetchList() {
  query.page = 1
  try {
    const data = await listResource({ ...query })
    list.value  = data.list  || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.warning(e?.message || '获取资源列表失败，请稍后重试')
  }
}
async function fetchFav() {
  try {
    const d = await listMyFavorites({ page: 1, size: 1000 })
    favMapInit(d.list || [])
  } catch (_) {}
}
function favMapInit(arr) {
  for (const k of Object.keys(favMap)) delete favMap[k]
  for (const it of arr) if (it.resourceId) favMap[it.resourceId] = true
}
function applyLocalFilters() {}
function resetAdv() {
  advQuery.uploaderRole = ''; advQuery.createdRange = []; advQuery.onlyFav = false
}
function onFilter(q) { Object.assign(query, q); fetchList() }
function onPage({ page, size }) { query.page = page; query.size = size; fetchList() }
function goDetail(r) { router.push(`/resource/${r.id}`) }

async function onQuickDownload(r) {
  if (Number(r.reviewStatus) !== 1) {
    ElMessage.warning('该资源尚未通过审核，暂不可下载')
    return
  }
  try {
    await downloadResource(r.id, r.originalName || r.fileName || `${r.title}`)
  } catch (e) {
    if (e && e.code === 401) {
      ElMessage.warning('请先登录后再下载资源')
    } else {
      ElMessage.warning(e?.message || '下载失败，请稍后重试')
    }
  }
}

async function toggleFav(r) {
  try {
    if (favMap[r.id]) {
      await removeFavorite(r.id)
      delete favMap[r.id]
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(r.id)
      favMap[r.id] = true
      ElMessage.success('已加入收藏')
    }
  } catch (e) {
    ElMessage.warning(e?.message || '收藏操作失败，请稍后重试')
  }
}

onMounted(() => {
  fetchList()
  fetchFav()
})
</script>

<style scoped>
.text-muted{ color: var(--qz-text-secondary); }
.text-primary { color: var(--qz-primary); }
.text-sm{ font-size: 13px; }
.mb-12 { margin-bottom: 12px; }
.mb-16 { margin-bottom: 16px; }
.ml-8  { margin-left: 8px; }
.gap-8 { gap: 8px; }
.flex { display: flex; }
.flex-wrap { flex-wrap: wrap; }
.items-center { align-items: center; }
.adv-collapse :deep(.el-collapse-item__header) { font-weight: 500; }
.adv-form { padding: 8px 4px 0; }
</style>
