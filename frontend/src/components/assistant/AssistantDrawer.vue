<script setup>
import AssistantComposer from './AssistantComposer.vue'
import AssistantHeader from './AssistantHeader.vue'
import AssistantMessageList from './AssistantMessageList.vue'
import AssistantWelcome from './AssistantWelcome.vue'
import AssistantMascot from './AssistantMascot.vue'
import { useCampusAssistant } from '../../composables/useCampusAssistant'

const assistant = useCampusAssistant()
</script>

<template>
  <aside class="assistant-drawer" role="dialog" aria-modal="false" aria-label="青禾校园助手">
    <AssistantHeader :status="assistant.serviceStatus.value" :has-messages="Boolean(assistant.messages.value.length)" @clear="assistant.clearConversation" @minimize="assistant.minimizeAssistant" @close="assistant.closeAssistant" />
    <div class="drawer-mascot-state"><AssistantMascot compact :state="assistant.mascotState.value" /><span>{{ assistant.mascotState.value === 'thinking' ? '小助手正在思考…' : assistant.mascotState.value === 'speaking' ? '小助手正在整理回复…' : '小助手随时准备帮忙' }}</span></div>
    <el-alert v-if="!assistant.isLoggedIn.value" class="login-tip" title="未登录也可以查询公开信息；涉及“我的”信息时请先登录。" type="info" :closable="false" show-icon />
    <AssistantWelcome v-if="!assistant.messages.value.length" :disabled="assistant.loading.value" @ask="assistant.sendMessage" />
    <AssistantMessageList v-else :messages="assistant.messages.value" @resend="assistant.sendMessage" />
    <AssistantComposer :model-value="assistant.input.value" :loading="assistant.loading.value" @update:model-value="assistant.input.value = $event" @send="assistant.sendMessage" @stop="assistant.stopGeneration" />
  </aside>
</template>

<style scoped>
.assistant-drawer{position:fixed;top:20px;right:20px;bottom:20px;z-index:1901;display:flex;flex-direction:column;width:420px;max-width:calc(100vw - 40px);overflow:hidden;border:1px solid rgba(211,222,239,.92);border-radius:16px;background:#fff;box-shadow:0 20px 48px rgba(35,59,104,.20)}.drawer-mascot-state{display:flex;align-items:center;gap:7px;min-height:42px;padding:0 15px;border-bottom:1px solid rgba(224,231,243,.8);color:var(--qh-text-secondary);font-size:12px}.drawer-mascot-state :deep(.assistant-mascot){flex:0 0 34px;width:34px}.login-tip{margin:12px 16px 0}.login-tip :deep(.el-alert__title){font-size:12px;line-height:1.45}@media(max-width:640px){.assistant-drawer{top:0;right:0;bottom:0;left:0;width:auto;max-width:none;border:0;border-radius:0}.login-tip{margin:10px 14px 0}}
</style>
