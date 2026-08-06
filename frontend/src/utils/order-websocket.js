import { getAdminToken } from './admin-session'
import { getToken } from './token'

const MAX_RECONNECT_ATTEMPTS = 5
const MAX_RECONNECT_DELAY = 8000
const connections = new Map()

function diagnostic(channel, event, details = {}) {
  console.info('[OrderWebSocket]', { channel, event, ...details })
}

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
  if (!tokenFor(channel)) {
    diagnostic(channel, 'skipped-no-session')
    return () => {}
  }

  const state = { channel, onMessage, stopped: false, attempts: 0, socket: null, timer: null, seen: new Set() }
  connections.set(channel, state)

  const open = () => {
    const token = tokenFor(channel)
    if (state.stopped || !token) {
      diagnostic(channel, 'reconnect-stopped-or-session-cleared')
      return
    }
    const url = socketUrl(channel)
    diagnostic(channel, state.attempts ? 'reconnecting' : 'connecting', {
      url,
      subprotocol: channel === 'admin' ? 'qh-admin.{redacted}' : 'qh-user.{redacted}',
      attempt: state.attempts
    })
    const socket = new WebSocket(url, [protocolFor(channel, token)])
    state.socket = socket
    socket.onopen = () => {
      state.attempts = 0
      diagnostic(channel, 'connected')
    }
    socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        const key = [message.messageType, message.orderId, message.orderStatus, message.occurredAt].join('|')
        if (state.seen.has(key)) {
          diagnostic(channel, 'duplicate-message-ignored', { messageType: message.messageType, orderId: message.orderId })
          return
        }
        state.seen.add(key)
        if (state.seen.size > 100) state.seen.delete(state.seen.values().next().value)
        diagnostic(channel, 'message-received', {
          messageType: message.messageType,
          orderId: message.orderId,
          orderNo: message.orderNo,
          summary: message.summary,
          occurredAt: message.occurredAt
        })
        state.onMessage(message)
      } catch (error) {
        console.warn('[OrderWebSocket] malformed-message-ignored', { channel, error: error?.message })
      }
    }
    socket.onerror = () => {
      diagnostic(channel, 'socket-error')
      socket.close()
    }
    socket.onclose = (event) => {
      diagnostic(channel, 'disconnected', { code: event.code, reason: event.reason || undefined, intentional: state.stopped })
      if (state.stopped || state.attempts >= MAX_RECONNECT_ATTEMPTS) {
        if (!state.stopped) diagnostic(channel, 'reconnect-exhausted')
        return
      }
      const delay = Math.min(1000 * (2 ** state.attempts), MAX_RECONNECT_DELAY)
      state.attempts += 1
      diagnostic(channel, 'reconnect-scheduled', { delay, attempt: state.attempts })
      state.timer = window.setTimeout(open, delay)
    }
  }
  open()
  return () => disconnectOrderWebSocket(channel)
}

export function disconnectOrderWebSocket(channel) {
  const state = connections.get(channel)
  if (!state) return
  diagnostic(channel, 'disconnect-requested')
  state.stopped = true
  if (state.timer) window.clearTimeout(state.timer)
  if (state.socket && (state.socket.readyState === WebSocket.OPEN || state.socket.readyState === WebSocket.CONNECTING)) state.socket.close()
  connections.delete(channel)
}
