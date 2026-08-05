<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import AssistantMascot from './AssistantMascot.vue'

const props = defineProps({
  unread: Boolean,
  cardVisible: Boolean,
  mascotState: { type: String, default: 'idle' }
})
const emit = defineEmits(['activate', 'start-consultation', 'dismiss-card'])

const POSITION_KEY = 'qh-campus-assistant-floating-position'
const EDGE_PADDING = 12
const DRAG_THRESHOLD = 5
const anchor = ref(null)
const ready = ref(false)
const dragging = ref(false)
const position = ref({ x: 0, y: 0 })
let activePointerId = null
let startPointer = null
let startPosition = null
let dragged = false
let ignoreNextClick = false

const anchorStyle = computed(() => ({ transform: `translate3d(${position.value.x}px, ${position.value.y}px, 0)` }))
const cardClasses = computed(() => ({
  'opens-down': position.value.y < 270,
  'align-start': position.value.x < 250
}))

function buttonSize() {
  const button = anchor.value?.querySelector('.assistant-floating-button')
  return Math.round(button?.getBoundingClientRect().width || 56)
}

function clampPosition(next) {
  const size = buttonSize()
  return {
    x: Math.round(Math.min(Math.max(EDGE_PADDING, next.x), Math.max(EDGE_PADDING, window.innerWidth - size - EDGE_PADDING))),
    y: Math.round(Math.min(Math.max(EDGE_PADDING, next.y), Math.max(EDGE_PADDING, window.innerHeight - size - EDGE_PADDING)))
  }
}

function defaultPosition() {
  const size = buttonSize()
  return clampPosition({ x: window.innerWidth - size - 28, y: window.innerHeight - size - 28 })
}

function savePosition() {
  localStorage.setItem(POSITION_KEY, JSON.stringify(position.value))
}

function restorePosition() {
  try {
    const stored = JSON.parse(localStorage.getItem(POSITION_KEY) || 'null')
    position.value = stored && Number.isFinite(stored.x) && Number.isFinite(stored.y)
      ? clampPosition(stored)
      : defaultPosition()
  } catch {
    position.value = defaultPosition()
  }
}

function onPointerDown(event) {
  if (event.pointerType === 'mouse' && event.button !== 0) return
  activePointerId = event.pointerId
  startPointer = { x: event.clientX, y: event.clientY }
  startPosition = { ...position.value }
  dragged = false
  event.currentTarget.setPointerCapture?.(event.pointerId)
}

function onPointerMove(event) {
  if (event.pointerId !== activePointerId || !startPointer || !startPosition) return
  const dx = event.clientX - startPointer.x
  const dy = event.clientY - startPointer.y
  if (!dragged && Math.hypot(dx, dy) <= DRAG_THRESHOLD) return
  dragged = true
  dragging.value = true
  position.value = clampPosition({ x: startPosition.x + dx, y: startPosition.y + dy })
}

function releasePointer(event, persist = true) {
  if (event.pointerId !== activePointerId) return
  event.currentTarget.releasePointerCapture?.(event.pointerId)
  if (dragged && persist) {
    savePosition()
    ignoreNextClick = true
    window.setTimeout(() => { ignoreNextClick = false }, 0)
  }
  activePointerId = null
  startPointer = null
  startPosition = null
  dragging.value = false
}

function onPointerUp(event) {
  releasePointer(event)
}

function onPointerCancel(event) {
  releasePointer(event, false)
  dragged = false
  ignoreNextClick = false
}

function onButtonClick(event) {
  if (ignoreNextClick || dragged) {
    event.preventDefault()
    event.stopPropagation()
    ignoreNextClick = false
    dragged = false
    return
  }
  emit('activate')
}

function onResize() {
  position.value = clampPosition(position.value)
}

onMounted(async () => {
  await nextTick()
  restorePosition()
  ready.value = true
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => window.removeEventListener('resize', onResize))
</script>

<template>
  <div ref="anchor" v-show="ready" class="assistant-floating-anchor" :style="anchorStyle">
    <section v-if="cardVisible" class="assistant-mascot-card" :class="cardClasses" aria-label="青禾校园助手">
      <button class="mascot-close" type="button" aria-label="关闭助手人物卡片" @click="$emit('dismiss-card')">×</button>
      <AssistantMascot :state="mascotState" />
      <div class="mascot-copy"><strong>青禾校园小助手</strong><span>{{ mascotState === 'thinking' ? '正在认真思考…' : mascotState === 'speaking' ? '正在和你说话…' : '想查校园生活信息，随时来找我。' }}</span></div>
      <button class="mascot-start" type="button" @click="$emit('start-consultation')">开始咨询</button>
    </section>
    <button class="assistant-floating-button" :class="{ dragging }" type="button" aria-label="打开青禾校园助手" @pointerdown="onPointerDown" @pointermove="onPointerMove" @pointerup="onPointerUp" @pointercancel="onPointerCancel" @click="onButtonClick">
      <svg viewBox="0 0 48 48" aria-hidden="true"><path d="M14 23v-4a10 10 0 0 1 20 0v4" /><rect x="10" y="21" width="28" height="19" rx="9" /><path d="M18 30h.1M30 30h.1M20 35c2.7 1.6 5.3 1.6 8 0" /><path d="M24 9V5M21 5h6" /></svg>
      <span v-if="unread" class="unread-dot" aria-label="有新消息" />
    </button>
  </div>
</template>

<style scoped>
.assistant-floating-anchor{position:fixed;top:0;left:0;z-index:1900;will-change:transform}.assistant-floating-button{position:relative;display:grid;place-items:center;width:56px;height:56px;padding:0;border:0;border-radius:18px;color:#fff;background:linear-gradient(135deg,var(--qh-primary),#6f9af0);box-shadow:0 12px 28px rgba(45,94,183,.30);cursor:grab;touch-action:none;user-select:none;transition:transform .18s ease,box-shadow .18s ease}.assistant-floating-button:hover,.assistant-floating-button:focus-visible{transform:scale(1.05);box-shadow:0 16px 32px rgba(45,94,183,.38);outline:none}.assistant-floating-button:focus-visible{outline:3px solid rgba(61,120,216,.30);outline-offset:3px}.assistant-floating-button.dragging{cursor:grabbing;transition:none}.assistant-floating-button svg{width:31px;height:31px;fill:none;stroke:currentColor;stroke-width:2.4;stroke-linecap:round;stroke-linejoin:round}.unread-dot{position:absolute;top:6px;right:6px;width:10px;height:10px;border:2px solid #fff;border-radius:50%;background:#f56c6c}.assistant-mascot-card{position:absolute;right:0;bottom:calc(100% + 12px);display:grid;justify-items:center;width:250px;padding:14px;border:1px solid rgba(205,221,246,.95);border-radius:20px;background:linear-gradient(145deg,#fff,#f5f8ff);box-shadow:0 18px 42px rgba(35,59,104,.22);text-align:center}.assistant-mascot-card.align-start{right:auto;left:0}.assistant-mascot-card.opens-down{top:calc(100% + 12px);bottom:auto}.mascot-close{position:absolute;top:8px;right:10px;width:26px;height:26px;padding:0;border:0;border-radius:50%;color:#71809a;background:transparent;font-size:22px;line-height:1;cursor:pointer}.mascot-close:hover,.mascot-close:focus-visible{color:var(--qh-primary);background:#eaf1ff;outline:none}.mascot-copy{display:grid;gap:5px;margin-top:-4px}.mascot-copy strong{color:var(--qh-text);font-size:15px}.mascot-copy span{min-height:34px;color:var(--qh-text-secondary);font-size:12px;line-height:1.45}.mascot-start{width:100%;margin-top:12px;padding:9px 14px;border:0;border-radius:11px;color:#fff;background:var(--qh-primary);font-weight:700;cursor:pointer}.mascot-start:hover,.mascot-start:focus-visible{background:#2f65b9;outline:3px solid rgba(61,120,216,.24);outline-offset:2px}@media(max-width:640px){.assistant-floating-button{width:50px;height:50px;border-radius:16px}.assistant-mascot-card{width:min(250px,calc(100vw - 24px))}}
</style>
