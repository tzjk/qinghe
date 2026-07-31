import { computed, ref } from 'vue'
import { clearAgentConversation, streamAgentChat } from '../api/agent'
import { getAssistantToolLabel } from '../components/assistant/assistant-tool-labels'
import { useUserStore } from '../stores/user'
import { clearAgentConversationId, getAgentConversationId, setAgentConversationId } from '../utils/agent-session'

const isOpen = ref(false)
const isMinimized = ref(false)
const messages = ref([])
const input = ref('')
const loading = ref(false)
const currentToolStatus = ref('')
const conversationId = ref(getAgentConversationId())
const error = ref('')
const hasUnread = ref(false)
let controller = null
let messageSequence = 0

const createMessage = (role, text = '', extra = {}) => ({ id: ++messageSequence, role, text, ...extra })

function needsLogin(message) {
  return /(我的|我\s*的|个人资料|宿舍信息|最近订单|订单详情|优惠券余额|我的优惠券|默认地址)/.test(message)
}

function toSafeError(message) {
  const source = String(message || '')
  if (/登录|未登录|unauthori[sz]ed|\b401\b/i.test(source)) return '查询个人信息请先登录'
  if (/业务|spring|tool|校园服务/i.test(source)) return '校园业务服务暂时不可用'
  return '校园助手暂时不可用'
}

function addSystemMessage(text, kind = 'info', retryText = '') {
  messages.value.push(createMessage('system', text, { kind, retryText }))
  if (!isOpen.value || isMinimized.value) hasUnread.value = true
}

function removePlaceholder(assistant) {
  if (assistant.text || assistant.status) return
  messages.value = messages.value.filter((item) => item.id !== assistant.id)
}

function openAssistant() {
  isOpen.value = true
  isMinimized.value = false
  hasUnread.value = false
}

function closeAssistant() {
  isOpen.value = false
  isMinimized.value = false
  hasUnread.value = false
}

function minimizeAssistant() {
  isMinimized.value = true
}

function stopGeneration() {
  controller?.abort()
}

function resetLocalConversation() {
  stopGeneration()
  controller = null
  loading.value = false
  currentToolStatus.value = ''
  error.value = ''
  messages.value = []
  input.value = ''
  conversationId.value = ''
  hasUnread.value = false
  clearAgentConversationId()
}

async function sendMessage(question = input.value) {
  const user = useUserStore()
  const message = String(question || '').trim()
  if (!message || loading.value) return
  if (!user.isLoggedIn && needsLogin(message)) {
    input.value = message
    error.value = '查询个人信息请先登录'
    addSystemMessage(error.value, 'warning', message)
    return
  }

  error.value = ''
  input.value = ''
  loading.value = true
  currentToolStatus.value = ''
  messages.value.push(createMessage('user', message))
  const assistant = createMessage('assistant', '', { status: '' })
  messages.value.push(assistant)
  const activeController = new AbortController()
  controller = activeController

  try {
    await streamAgentChat({
      message,
      conversationId: conversationId.value,
      signal: activeController.signal,
      onEvent(event, data) {
        if (event === 'conversation.started' && data?.conversation_id) {
          conversationId.value = data.conversation_id
          setAgentConversationId(data.conversation_id)
        }
        if (event === 'tool.started') {
          const label = getAssistantToolLabel(data?.tool_name)
          currentToolStatus.value = label
          assistant.status = label
        }
        if (event === 'tool.completed' || event === 'tool.failed') {
          currentToolStatus.value = ''
          assistant.status = ''
        }
        if (event === 'answer.delta') {
          assistant.status = ''
          assistant.text += String(data?.text || '')
          if (!isOpen.value || isMinimized.value) hasUnread.value = true
        }
        if (event === 'error') {
          currentToolStatus.value = ''
          assistant.status = ''
          const safeError = toSafeError(data?.message)
          error.value = safeError
          removePlaceholder(assistant)
          input.value = message
          addSystemMessage(safeError, 'error', message)
        }
      }
    })
    if (controller === activeController && !assistant.text && !error.value) {
      removePlaceholder(assistant)
      addSystemMessage('暂未收到可展示的回答，请稍后重新发送。', 'warning', message)
    }
  } catch (cause) {
    if (controller !== activeController) return
    currentToolStatus.value = ''
    assistant.status = ''
    if (cause?.name === 'AbortError') {
      removePlaceholder(assistant)
      addSystemMessage('已停止生成。', 'info', message)
    } else {
      const safeError = toSafeError(cause?.message)
      error.value = safeError
      removePlaceholder(assistant)
      input.value = message
      addSystemMessage(safeError, 'error', message)
    }
  } finally {
    if (controller === activeController) {
      controller = null
      loading.value = false
      currentToolStatus.value = ''
      assistant.status = ''
    }
  }
}

async function clearConversation() {
  const activeConversationId = conversationId.value
  resetLocalConversation()
  await clearAgentConversation(activeConversationId)
}

export function stopCampusAssistantGeneration() {
  stopGeneration()
}

export function resetCampusAssistantConversation() {
  resetLocalConversation()
}

export function useCampusAssistant() {
  return {
    isOpen,
    isMinimized,
    messages,
    input,
    loading,
    currentToolStatus,
    conversationId,
    error,
    hasUnread,
    isLoggedIn: computed(() => useUserStore().isLoggedIn),
    serviceStatus: computed(() => loading.value ? '正在查询' : (error.value ? '校园助手暂时不可用' : '服务正常')),
    openAssistant,
    closeAssistant,
    minimizeAssistant,
    sendMessage,
    stopGeneration,
    clearConversation
  }
}
