<template>
  <ResourcePublish />
</template>

<script setup>
// 路由 /resource/:id/update 复用发布页面；内部按 route.params.id 自动进入编辑模式
import ResourcePublish from './ResourcePublish.vue'
</script>
