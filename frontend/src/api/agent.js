import { getToken } from '../utils/token'

const baseUrl = (import.meta.env.VITE_AGENT_BASE_URL || 'http://127.0.0.1:8100').replace(/\/$/, '')

export async function streamAgentChat({ message, conversationId, signal, onEvent }) {
  const headers = { 'Content-Type': 'application/json', Accept: 'text/event-stream' }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  const response = await fetch(`${baseUrl}/api/v1/chat/stream`, {
    method: 'POST', headers, signal,
    body: JSON.stringify({ message, conversation_id: conversationId || undefined })
  })
  if (!response.ok || !response.body) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.error?.message || '校园助手暂时不可用')
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  try {
    while (true) {
      const { value, done } = await reader.read()
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
      const blocks = buffer.split(/\r?\n\r?\n/)
      buffer = blocks.pop() || ''
      for (const block of blocks) parseSse(block, onEvent)
      if (done) break
    }
    if (buffer.trim()) parseSse(buffer, onEvent)
  } finally { reader.releaseLock() }
}

function parseSse(block, onEvent) {
  const lines = block.split(/\r?\n/)
  const event = lines.find(line => line.startsWith('event:'))?.slice(6).trim()
  const data = lines.filter(line => line.startsWith('data:')).map(line => line.slice(5).trim()).join('\n')
  if (!event || !data) return
  try { onEvent(event, JSON.parse(data)) } catch { /* malformed events are ignored without exposing internals */ }
}

export async function clearAgentConversation(conversationId) {
  if (!conversationId) return
  const token = getToken()
  const headers = token ? { Authorization: `Bearer ${token}` } : undefined
  await fetch(`${baseUrl}/api/v1/conversations/${encodeURIComponent(conversationId)}`, { method: 'DELETE', headers }).catch(() => undefined)
}
