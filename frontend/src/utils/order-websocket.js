import { getAdminToken } from './admin-session'
import { getToken } from './token'

const MAX_RECONNECT_ATTEMPTS = 5
const MAX_RECONNECT_DELAY = 8000
const connections = new Map()

function socketUrl(channel) {
  const configured = import.meta.env.VITE_WS_BASE_URL
  if (configured) return `${configured.replace(/\/$/, '')}/ws/orders/${channel}`
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/orders/${channel}`
}

function tokenFor(channel) {
  return channel === 'admin' ? getAdminToken() : getToken()
}

function protocolFor(channel, token) {
  return `${channel === 'admin' ? 'qh-admin.' : 'qh-user.'}${token}`
}

export function connectOrderWebSocket(channel, onMessage) {
  disconnectOrderWebSocket(channel)
  const token = tokenFor(channel)
  if (!token) return () => {}

  const state = { channel, onMessage, stopped: false, attempts: 0, socket: null, timer: null, seen: new Set() }
  connections.set(channel, state)

  const open = () => {
    if (state.stopped || !tokenFor(channel)) return
    const socket = new WebSocket(socketUrl(channel), [protocolFor(channel, token)])
    state.socket = socket
    socket.onopen = () => { state.attempts = 0 }
    socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        const key = [message.messageType, message.orderId, message.orderStatus, message.occurredAt].join('|')
        if (state.seen.has(key)) return
        state.seen.add(key)
        if (state.seen.size > 100) state.seen.delete(state.seen.values().next().value)
        state.onMessage(message)
      } catch {
        // Ignore malformed frames; order data is always refreshed from the HTTP API when the page loads or reconnects.
      }
    }
    socket.onerror = () => socket.close()
    socket.onclose = () => {
      if (state.stopped || state.attempts >= MAX_RECONNECT_ATTEMPTS) return
      const delay = Math.min(1000 * (2 ** state.attempts), MAX_RECONNECT_DELAY)
      state.attempts += 1
      state.timer = window.setTimeout(open, delay)
    }
  }
  open()
  return () => disconnectOrderWebSocket(channel)
}

export function disconnectOrderWebSocket(channel) {
  const state = connections.get(channel)
  if (!state) return
  state.stopped = true
  if (state.timer) window.clearTimeout(state.timer)
  if (state.socket && (state.socket.readyState === WebSocket.OPEN || state.socket.readyState === WebSocket.CONNECTING)) state.socket.close()
  connections.delete(channel)
}
