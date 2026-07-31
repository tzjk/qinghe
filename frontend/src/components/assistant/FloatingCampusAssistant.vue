<script setup>
import { onBeforeUnmount } from 'vue'
import { useCampusAssistant } from '../../composables/useCampusAssistant'
import AssistantDrawer from './AssistantDrawer.vue'
import FloatingAssistantButton from './FloatingAssistantButton.vue'

const assistant = useCampusAssistant()
onBeforeUnmount(assistant.stopGeneration)
</script>

<template>
  <Transition name="assistant-slide"><AssistantDrawer v-if="assistant.isOpen.value && !assistant.isMinimized.value" /></Transition>
  <FloatingAssistantButton v-if="!assistant.isOpen.value || assistant.isMinimized.value" :unread="assistant.hasUnread.value" @open="assistant.openAssistant" />
</template>

<style scoped>
.assistant-slide-enter-active,.assistant-slide-leave-active{transition:opacity .22s ease,transform .22s ease}.assistant-slide-enter-from,.assistant-slide-leave-to{opacity:0;transform:translateX(24px)}@media(max-width:640px){.assistant-slide-enter-from,.assistant-slide-leave-to{transform:translateY(18px)}}
</style>
