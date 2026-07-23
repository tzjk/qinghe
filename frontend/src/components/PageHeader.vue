<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, required: true },
  eyebrow: { type: String, default: '' }
})
</script>

<template>
  <header class="page-heading">
    <div>
      <p v-if="eyebrow" class="page-eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p class="subtle">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="header-actions"><slot name="actions" /></div>
  </header>
</template>

<style scoped>
.header-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
</style>
