<script setup>
defineProps({ label: { type: String, default: '' } })
</script>

<template>
  <span v-if="label" class="tool-status"><i aria-hidden="true" />{{ label }}</span>
</template>

<style scoped>
.tool-status{display:inline-flex;align-items:center;gap:7px;margin-top:7px;color:var(--qh-primary);font-size:12px}.tool-status i{width:7px;height:7px;border-radius:50%;background:currentColor;animation:soft-pulse 1.35s ease-in-out infinite}@keyframes soft-pulse{50%{opacity:.4;transform:scale(.72)}}
</style>
