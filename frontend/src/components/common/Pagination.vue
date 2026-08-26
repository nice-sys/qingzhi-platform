<template>
  <div class="pagination-wrap flex-between mt-16" v-if="total > 0">
    <div class="total text-muted text-sm">
      共 <b class="text-primary">{{ total }}</b> 条 / 第 <b class="text-primary">{{ current }}</b> 页
    </div>
    <el-pagination
      background
      layout="prev, pager, next, jumper, sizes"
      :current-page="current"
      :page-size="size"
      :page-sizes="sizes"
      :total="total"
      @current-change="p => emit('change', { page: p, size })"
      @size-change="s => emit('change', { page: 1, size: s })"
    />
  </div>
</template>

<script setup>
defineProps({
  total:   { type: Number, default: 0 },
  current: { type: Number, default: 1 },
  size:    { type: Number, default: 10 },
  sizes:   { type: Array,  default: () => [10, 20, 50, 100] }
})
const emit = defineEmits(['change'])
</script>

<style scoped>
.text-muted{ color: var(--qz-text-secondary); }
.text-primary{ color: var(--qz-primary); }
.text-sm{ font-size: 13px; }
.mt-16{ margin-top: 16px; }
</style>
