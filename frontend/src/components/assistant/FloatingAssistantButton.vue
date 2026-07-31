<script setup>
defineProps({ unread: Boolean })
defineEmits(['open'])
</script>

<template>
  <button class="assistant-floating-button" type="button" aria-label="打开青禾校园助手" @click="$emit('open')">
    <svg viewBox="0 0 48 48" aria-hidden="true"><path d="M14 23v-4a10 10 0 0 1 20 0v4" /><rect x="10" y="21" width="28" height="19" rx="9" /><path d="M18 30h.1M30 30h.1M20 35c2.7 1.6 5.3 1.6 8 0" /><path d="M24 9V5M21 5h6" /></svg>
    <span v-if="unread" class="unread-dot" aria-label="有新消息" />
  </button>
</template>

<style scoped>
.assistant-floating-button{position:fixed;right:28px;bottom:28px;z-index:1900;display:grid;place-items:center;width:56px;height:56px;padding:0;border:0;border-radius:18px;color:#fff;background:linear-gradient(135deg,var(--qh-primary),#6f9af0);box-shadow:0 12px 28px rgba(45,94,183,.30);cursor:pointer;transition:transform .18s ease,box-shadow .18s ease}.assistant-floating-button:hover,.assistant-floating-button:focus-visible{transform:scale(1.05);box-shadow:0 16px 32px rgba(45,94,183,.38);outline:none}.assistant-floating-button:focus-visible{outline:3px solid rgba(61,120,216,.30);outline-offset:3px}.assistant-floating-button svg{width:31px;height:31px;fill:none;stroke:currentColor;stroke-width:2.4;stroke-linecap:round;stroke-linejoin:round}.unread-dot{position:absolute;top:6px;right:6px;width:10px;height:10px;border:2px solid #fff;border-radius:50%;background:#f56c6c}@media(max-width:640px){.assistant-floating-button{right:16px;bottom:16px;width:50px;height:50px;border-radius:16px}}
</style>
