<script setup>
import { nextTick, ref, watch } from 'vue'
import AssistantMessageItem from './AssistantMessageItem.vue'

const props = defineProps({ messages: { type: Array, required: true } })
defineEmits(['resend'])
const list = ref(null)

watch(() => props.messages, async () => {
  await nextTick()
  if (list.value) list.value.scrollTop = list.value.scrollHeight
}, { deep: true })
</script>

<template>
  <main ref="list" class="assistant-message-list" aria-live="polite">
    <AssistantMessageItem v-for="item in messages" :key="item.id" :item="item" @resend="$emit('resend', $event)" />
  </main>
</template>

<style scoped>
.assistant-message-list{flex:1;min-height:0;padding:2px 18px 14px;overflow-y:auto;scroll-behavior:smooth}
</style>
