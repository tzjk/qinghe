<script setup>
defineProps({
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  empty: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无可展示内容' },
  skeletonRows: { type: Number, default: 6 }
})
defineEmits(['retry'])
</script>

<template>
  <el-skeleton v-if="loading" :rows="skeletonRows" animated />
  <div v-else-if="error" class="state-panel state-error">
    <el-result icon="error" title="加载失败" :sub-title="error">
      <template #extra><el-button type="primary" @click="$emit('retry')">重新加载</el-button></template>
    </el-result>
  </div>
  <div v-else-if="empty" class="state-panel"><el-empty :description="emptyText"><slot name="empty-action" /></el-empty></div>
  <slot v-else />
</template>
