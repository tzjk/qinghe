<script setup>
import AssistantToolStatus from './AssistantToolStatus.vue'

defineProps({ item: { type: Object, required: true } })
defineEmits(['resend'])
</script>

<template>
  <article v-if="item.role !== 'system' && (item.text || item.status)" :class="['assistant-message', item.role]">
    <div class="message-avatar" aria-hidden="true">{{ item.role === 'user' ? '我' : '禾' }}</div>
    <div class="message-bubble"><p v-if="item.text">{{ item.text }}</p><AssistantToolStatus :label="item.status" /></div>
  </article>
  <div v-else-if="item.role === 'system'" :class="['system-message', item.kind]" role="status">
    <span>{{ item.text }}</span><el-button v-if="item.retryText" link type="primary" size="small" @click="$emit('resend', item.retryText)">重新发送</el-button>
  </div>
</template>

<style scoped>
.assistant-message{display:flex;align-items:flex-start;gap:9px;margin:13px 0}.assistant-message.user{flex-direction:row-reverse}.message-avatar{display:grid;place-items:center;flex:0 0 28px;width:28px;height:28px;border-radius:50%;color:var(--qh-primary);background:#e9f0ff;font-size:12px;font-weight:800}.user .message-avatar{color:#fff;background:var(--qh-primary)}.message-bubble{max-width:88%;padding:10px 12px;border-radius:4px 14px 14px;background:#f2f5fa;color:var(--qh-text);line-height:1.58;word-break:break-word}.message-bubble p{margin:0;white-space:pre-wrap}.user .message-bubble{max-width:80%;border-radius:14px 4px 14px 14px;color:#fff;background:var(--qh-primary)}.system-message{display:flex;align-items:center;justify-content:space-between;gap:8px;margin:12px 0;padding:9px 10px;border-radius:10px;color:#7c5d13;background:#fff8e7;font-size:12px;line-height:1.45}.system-message.error{color:#a63b3b;background:#fff1f1}.system-message.info{color:var(--qh-text-secondary);background:#f4f6f9}
</style>
