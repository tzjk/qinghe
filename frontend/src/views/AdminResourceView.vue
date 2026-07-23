<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PageScaffold from '../components/PageScaffold.vue'
const title = computed(() => useRoute().meta.title || '后台')
</script>
<template><PageScaffold :title="title" description="后台资源管理页面骨架，后续里程碑接入表格、表单和状态动作。" :cards="['筛选区域', '数据表格', '操作区域']" /></template>
