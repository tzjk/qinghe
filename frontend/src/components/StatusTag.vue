<script setup>
import { computed } from 'vue'

const props = defineProps({ status: { type: [String, Number, Boolean], default: '' }, kind: { type: String, default: 'goods' } })
const statusInfo = computed(() => {
  if (props.status === 'ON_SALE' || props.status === 1 || props.status === true) return { text: props.kind === 'shop' ? '营业中' : '在售', type: 'success' }
  if (props.status === 'OFF_SALE' || props.status === 0 || props.status === false) return { text: props.kind === 'shop' ? '暂停营业' : '已下架', type: 'info' }
  return { text: String(props.status || '未知状态'), type: 'info' }
})
</script>

<template><el-tag class="status-tag" :type="statusInfo.type" effect="light">{{ statusInfo.text }}</el-tag></template>
