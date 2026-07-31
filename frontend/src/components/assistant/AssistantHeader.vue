<script setup>
import { Close, Delete, Minus } from '@element-plus/icons-vue'

defineProps({ status: { type: String, required: true }, hasMessages: Boolean })
defineEmits(['clear', 'minimize', 'close'])
</script>

<template>
  <header class="assistant-header">
    <div class="header-title"><span class="header-bot" aria-hidden="true"><svg viewBox="0 0 48 48"><path d="M14 23v-4a10 10 0 0 1 20 0v4" /><rect x="10" y="21" width="28" height="19" rx="9" /><path d="M18 30h.1M30 30h.1" /></svg></span><div><h1>青禾校园助手</h1><p>校园生活查询小帮手</p><small :class="{ working: status === '正在查询' || status.startsWith('正在查询') }">{{ status }}</small></div></div>
    <div class="header-actions"><el-button text circle title="清空会话" aria-label="清空会话" :disabled="!hasMessages" @click="$emit('clear')"><el-icon><Delete /></el-icon></el-button><el-button text circle title="最小化" aria-label="最小化助手" @click="$emit('minimize')"><el-icon><Minus /></el-icon></el-button><el-button text circle title="关闭" aria-label="关闭助手" @click="$emit('close')"><el-icon><Close /></el-icon></el-button></div>
  </header>
</template>

<style scoped>
.assistant-header{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;padding:18px 16px 14px;border-bottom:1px solid var(--qh-border)}.header-title{display:flex;gap:10px}.header-bot{display:grid;place-items:center;flex:0 0 38px;width:38px;height:38px;border-radius:13px;color:#fff;background:linear-gradient(135deg,var(--qh-primary),#7ea6f4)}.header-bot svg{width:25px;height:25px;fill:none;stroke:currentColor;stroke-width:2.4;stroke-linecap:round}.header-title h1{margin:0;color:var(--qh-text);font-size:16px;line-height:1.3}.header-title p{margin:2px 0;color:var(--qh-text-secondary);font-size:12px}.header-title small{display:block;color:#4e8d66;font-size:11px}.header-title small.working{color:var(--qh-primary)}.header-actions{display:flex;gap:0}.header-actions .el-button{margin:0;color:var(--qh-text-secondary)}
</style>
