<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useCampusAssistant } from '../../composables/useCampusAssistant'
import AssistantDrawer from './AssistantDrawer.vue'
import FloatingAssistantButton from './FloatingAssistantButton.vue'

const assistant = useCampusAssistant()
const mascotCardVisible = ref(false)
const waving = ref(false)
let waveTimer = null

const mascotState = computed(() => waving.value ? 'waving' : assistant.mascotState.value)

function showMascotCard() {
  mascotCardVisible.value = true
  waving.value = true
  if (waveTimer) window.clearTimeout(waveTimer)
  waveTimer = window.setTimeout(() => { waving.value = false; waveTimer = null }, 1300)
}

function dismissMascotCard() {
  mascotCardVisible.value = false
  waving.value = false
}

function startConsultation() {
  dismissMascotCard()
  assistant.openAssistant()
}

onBeforeUnmount(assistant.stopGeneration)
onBeforeUnmount(() => { if (waveTimer) window.clearTimeout(waveTimer) })
</script>

<template>
  <Transition name="assistant-slide"><AssistantDrawer v-if="assistant.isOpen.value && !assistant.isMinimized.value" /></Transition>
  <FloatingAssistantButton v-if="!assistant.isOpen.value || assistant.isMinimized.value" :unread="assistant.hasUnread.value" :card-visible="mascotCardVisible" :mascot-state="mascotState" @activate="showMascotCard" @dismiss-card="dismissMascotCard" @start-consultation="startConsultation" />
</template>

<style scoped>
.assistant-slide-enter-active,.assistant-slide-leave-active{transition:opacity .22s ease,transform .22s ease}.assistant-slide-enter-from,.assistant-slide-leave-to{opacity:0;transform:translateX(24px)}@media(max-width:640px){.assistant-slide-enter-from,.assistant-slide-leave-to{transform:translateY(18px)}}
</style>
