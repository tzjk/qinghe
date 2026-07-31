<script setup>
import { computed } from 'vue'

const props = defineProps({ modelValue: { type: String, default: '' }, loading: Boolean })
const emit = defineEmits(['update:modelValue', 'send', 'stop'])
const canSend = computed(() => Boolean(props.modelValue.trim()) && !props.loading)
function send() { if (canSend.value) emit('send') }
</script>

<template>
  <footer class="assistant-composer">
    <el-input :model-value="modelValue" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" maxlength="1000" show-word-limit resize="none" placeholder="例如：今天有什么优惠？" :disabled="loading" @update:model-value="emit('update:modelValue', $event)" @keydown.enter.exact.prevent="send" />
    <div class="composer-actions"><span>Enter 发送，Shift + Enter 换行</span><div><el-button v-if="loading" size="small" @click="emit('stop')">停止生成</el-button><el-button type="primary" size="small" :disabled="!canSend" @click="send">发送</el-button></div></div>
  </footer>
</template>

<style scoped>
.assistant-composer{margin-top:auto;padding:13px 16px 15px;border-top:1px solid var(--qh-border);background:#fff}.composer-actions{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-top:8px;color:var(--qh-text-secondary);font-size:11px}.composer-actions>div{display:flex;gap:8px}@media(max-width:640px){.assistant-composer{padding-bottom:max(15px,env(safe-area-inset-bottom))}.composer-actions>span{display:none}}
</style>
