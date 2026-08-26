<template>
  <div class="resource-filter qz-card mb-16">
    <el-form :model="model" inline size="default" label-position="right">
      <el-form-item label="关键词">
        <el-input
          v-model="model.keyword"
          placeholder="按标题/描述/上传者搜索"
          clearable
          style="width:260px"
          @keyup.enter="emitQuery"
          @clear="emitQuery"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="model.category" placeholder="全部课程分类" clearable style="width:180px" @change="emitQuery">
          <el-option v-for="c in CATEGORY_OPTIONS" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="资源类型">
        <el-select v-model="model.type" placeholder="全部类型" clearable style="width:160px" @change="emitQuery">
          <el-option v-for="t in RESOURCE_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="排序">
        <el-select v-model="model.sort" style="width:140px" @change="emitQuery">
          <el-option label="最新发布" value="newest" />
          <el-option label="下载最多" value="download" />
          <el-option label="收藏最多" value="favorite" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="emitQuery">
          <el-icon><Search /></el-icon>&nbsp;搜索
        </el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'
import { CATEGORY_OPTIONS, RESOURCE_TYPE_OPTIONS } from '@/utils/constants'

const props = defineProps({ defaultModel: { type: Object, default: () => ({}) } })
const emit = defineEmits(['query'])

const defaultState = {
  keyword: '', category: '', type: '', sort: 'newest',
  ...(props.defaultModel || {})
}
const model = reactive({ ...defaultState })

function emitQuery() {
  emit('query', { ...model, page: 1 })
}
function reset() {
  Object.assign(model, defaultState)
  emitQuery()
}

watch(() => props.defaultModel, (v) => {
  Object.assign(model, { ...defaultState, ...(v || {}) })
}, { deep: true })
</script>

<style scoped>
.mb-16 { margin-bottom: 16px; }
</style>
