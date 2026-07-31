const KEY = 'qh:agent:conversation_id'
export const getAgentConversationId = () => window.sessionStorage.getItem(KEY) || ''
export const setAgentConversationId = (value) => value && window.sessionStorage.setItem(KEY, value)
export const clearAgentConversationId = () => window.sessionStorage.removeItem(KEY)
