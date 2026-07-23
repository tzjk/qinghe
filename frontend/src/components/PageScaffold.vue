<script setup>
defineProps({ title: { type: String, required: true }, description: { type: String, required: true }, cards: { type: Array, default: () => [] }, showBack: { type: Boolean, default: false } })
</script>
<template><section class="page-shell"><div class="page-heading"><div><h1>{{ title }}</h1><p class="subtle">{{ description }}</p></div><slot name="actions"><el-button v-if="showBack" plain @click="$router.back()">返回</el-button></slot></div><div class="placeholder-grid"><el-card v-for="card in cards" :key="card" class="placeholder-card" shadow="never"><strong>{{ card }}</strong><p class="subtle">M1 已建立页面结构，业务数据将在后续里程碑接入。</p></el-card></div><el-empty description="当前没有可展示的数据" /></section></template>
