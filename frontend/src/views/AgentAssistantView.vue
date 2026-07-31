<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCampusAssistant } from '../composables/useCampusAssistant'

const router = useRouter()
const assistant = useCampusAssistant()

onMounted(() => {
  assistant.openAssistant()
  router.replace({ name: 'home' })
})
</script>

<template><span class="assistant-route-compatibility" aria-hidden="true" /></template>

<style scoped>
.assistant-route-compatibility{display:none}
</style>
