<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElNotification } from 'element-plus'
import { useAdminStore } from '../stores/admin'
import { connectOrderWebSocket, disconnectOrderWebSocket } from '../utils/order-websocket'

const MAX_UNREAD_MESSAGES = 50
const SUPPORTED_MESSAGE_TYPES = new Set(['ADMIN_NEW_ORDER', 'ADMIN_ORDER_REMINDER'])
const adminStore = useAdminStore()
const drawerVisible = ref(false)
const notifications = ref([])
const receivedKeys = new Set()
let closeSocket = () => {}

const unreadCount = computed(() => notifications.value.filter((item) => !item.read).length)

function messageKey(message) {
  return [message.messageType, message.orderId, message.orderNo, message.occurredAt].join(':')
}

function notificationTitle(message) {
  return message.messageType === 'ADMIN_ORDER_REMINDER' ? '客户催单提醒' : '来单提醒'
}

function notificationText(message) {
  const orderNo = message.orderNo || `#${message.orderId}`
  return `订单号：${orderNo}\n${message.summary || '您有一条新的订单通知'}`
}

function receive(message) {
  if (!SUPPORTED_MESSAGE_TYPES.has(message?.messageType)) {
    console.warn('[AdminOrderNotification] 忽略未知管理员订单消息', { messageType: message?.messageType })
    return
  }
  const key = messageKey(message)
  if (receivedKeys.has(key)) {
    console.info('[AdminOrderNotification] 忽略重复消息', { messageType: message.messageType, orderId: message.orderId })
    return
  }
  receivedKeys.add(key)
  const notification = { ...message, key, read: false }
  notifications.value.unshift(notification)
  if (notifications.value.length > MAX_UNREAD_MESSAGES) {
    notifications.value.splice(MAX_UNREAD_MESSAGES).forEach((item) => receivedKeys.delete(item.key))
  }
  console.info('[AdminOrderNotification] 收到管理员订单通知', {
    messageType: message.messageType,
    orderId: message.orderId,
    orderNo: message.orderNo,
    summary: message.summary,
    occurredAt: message.occurredAt
  })
  ElNotification({
    title: notificationTitle(message),
    message: notificationText(message),
    type: message.messageType === 'ADMIN_ORDER_REMINDER' ? 'warning' : 'success',
    position: 'top-right',
    duration: 8000
  })
  window.dispatchEvent(new CustomEvent('qinghe-admin-order-notification', { detail: message }))
}

function disconnect() {
  closeSocket()
  closeSocket = () => {}
  disconnectOrderWebSocket('admin')
}

function connect() {
  disconnect()
  if (!adminStore.token) return
  console.info('[AdminOrderNotification] 管理员已登录，初始化全局订单 WebSocket')
  closeSocket = connectOrderWebSocket('admin', receive)
}

function openNotificationDrawer() {
  notifications.value.forEach((item) => { item.read = true })
  drawerVisible.value = true
}

function clearNotifications() {
  notifications.value = []
  receivedKeys.clear()
}

watch(() => adminStore.token, (token) => {
  if (!token) {
    disconnect()
    clearNotifications()
    return
  }
  connect()
}, { immediate: true })

onBeforeUnmount(disconnect)
</script>

<template>
  <div v-if="adminStore.token" class="admin-order-notification-center">
    <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
      <el-button type="warning" @click="openNotificationDrawer">订单提醒</el-button>
    </el-badge>
  </div>
  <el-drawer v-model="drawerVisible" title="订单提醒" size="360px">
    <template #header>
      <div class="notification-drawer-header"><span>订单提醒</span><el-button link type="primary" @click="clearNotifications">清空</el-button></div>
    </template>
    <el-empty v-if="notifications.length === 0" description="暂无订单提醒" />
    <div v-else class="notification-list">
      <article v-for="item in notifications" :key="item.key" class="notification-item">
        <strong>{{ notificationTitle(item) }}</strong><p>订单号：{{ item.orderNo || `#${item.orderId}` }}</p><p>{{ item.summary }}</p><small>{{ item.occurredAt || '刚刚' }}</small>
      </article>
    </div>
  </el-drawer>
</template>

<style scoped>
.admin-order-notification-center { position: fixed; right: 24px; bottom: 24px; z-index: 2000; }
.notification-drawer-header { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.notification-list { display: grid; gap: 12px; }
.notification-item { padding: 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 8px; background: var(--el-fill-color-lighter); }
.notification-item p { margin: 6px 0; word-break: break-word; }
.notification-item small { color: var(--el-text-color-secondary); }
</style>
