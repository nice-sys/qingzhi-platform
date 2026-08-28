<template>
  <el-tag
    :type="tagType"
    size="default"
    effect="light"
    round
    class="status-tag"
  >
    <slot>{{ text }}</slot>
  </el-tag>
</template>

<script setup>
// 展示资源审核状态、用户封禁状态等状态标签
import { computed } from 'vue'
import { REVIEW } from '@/utils/permission'

const props = defineProps({
  // REVIEW_PENDING=0 / PASS=1 / REJECT=2
  status: { type: [Number, String], default: 0 },
  text:   { type: String, default: '' },
  kind:   { type: String, default: 'review' } // review | user-status
})

const REVIEW_TEXT = {
  [REVIEW.PENDING]: '待审核',
  [REVIEW.PASS]:    '已通过',
  [REVIEW.REJECT]:  '已拒绝',
  3:                '草稿'
}
const TEXT_MAP = { review: REVIEW_TEXT }

const textComputed = computed(() => props.text || (TEXT_MAP[props.kind] || {})[props.status] || '-')
const tagType = computed(() => {
  if (props.kind === 'review') {
    if (Number(props.status) === REVIEW.PENDING) return 'warning'
    if (Number(props.status) === REVIEW.PASS)    return 'success'
    if (Number(props.status) === REVIEW.REJECT)  return 'danger'
    if (Number(props.status) === 3)              return 'info'
  }
  return 'info'
})
defineExpose({ textComputed, tagType })
</script>

<style scoped>
.status-tag { font-size: 12px; }
</style>
