<template>
  <el-card class="import-result qz-card" shadow="never" v-if="result">
    <template #header>
      <div class="flex-between">
        <strong>📥 批量导入结果</strong>
        <el-tag :type="summaryTag" size="small" effect="plain">
          成功 {{ summary.success }} / 失败 {{ summary.fail }}
        </el-tag>
      </div>
    </template>

    <div class="summary mb-12">
      <el-descriptions :column="4" size="small" border>
        <el-descriptions-item label="总记录数">{{ summary.total }}</el-descriptions-item>
        <el-descriptions-item label="成功">{{ summary.success }}</el-descriptions-item>
        <el-descriptions-item label="失败">{{ summary.fail }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ (summary.costMs || 0) + ' ms' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div v-if="errors && errors.length">
      <h4 class="mb-8">⚠️ 失败明细</h4>
      <el-table :data="errors.slice(0, 50)" stripe size="small" max-height="320">
        <el-table-column prop="row" label="行号" width="80" />
        <el-table-column prop="key" label="标识" width="160" />
        <el-table-column prop="message" label="失败原因" show-overflow-tooltip />
      </el-table>
      <div v-if="errors.length > 50" class="text-muted text-sm mt-8">
        仅展示前 50 条，其余 {{ errors.length - 50 }} 条请参考后端日志。
      </div>
    </div>

    <div class="text-right mt-16">
      <slot name="actions">
        <el-button @click="$emit('close')">关闭</el-button>
      </slot>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 后端 AdminImportResponse: { total, success, fail, costMs?, errors?: [{row,key,message}] }
  result: { type: Object, default: null }
})
defineEmits(['close'])

const summary = computed(() => ({
  total: props.result?.total || 0,
  success: props.result?.successCount ?? props.result?.success ?? 0,
  fail: props.result?.failCount ?? props.result?.fail ?? 0,
  costMs: props.result?.costMs ?? props.result?.cost ?? 0
}))
const errors = computed(() => props.result?.errors || [])
const summaryTag = computed(() => {
  if (summary.value.fail === 0) return 'success'
  if (summary.value.success === 0) return 'danger'
  return 'warning'
})
</script>

<style scoped>
.text-muted{ color: var(--qz-text-secondary); }
.text-sm{ font-size: 13px; }
.text-right{ text-align: right; }
.mb-8{ margin-bottom: 8px; }
.mb-12{ margin-bottom: 12px; }
.mt-8{ margin-top: 8px; }
.mt-16{ margin-top: 16px; }
</style>
