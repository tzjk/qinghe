<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
const route = useRoute(); const router = useRouter()
const money = (value) => Number(value || 0).toFixed(2)
const order = computed(() => window.history.state?.order || null)
</script>

<template>
  <section class="page-shell"><PageHeader title="下单成功" description="订单已创建，后续支付与配送流程不在本轮范围内。" eyebrow="ORDER CREATED" /><el-card class="ui-card success-card"><el-result icon="success" title="订单创建成功" sub-title="服务端已确认商品、库存和应付金额。"><template #extra><el-descriptions v-if="order" :column="1" border><el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item><el-descriptions-item label="店铺">{{ order.shopName }}</el-descriptions-item><el-descriptions-item label="应付金额">¥{{ money(order.payAmount) }}</el-descriptions-item><el-descriptions-item label="配送地址">{{ order.addressSummary }}</el-descriptions-item></el-descriptions><el-alert v-else title="订单已创建；请返回购物车继续选购。" type="info" :closable="false" /><div class="actions"><el-button @click="router.push('/shops')">继续选购</el-button></div></template></el-result></el-card></section>
</template>

<style scoped>.success-card { max-width:720px; margin:0 auto; }.actions { margin-top:20px; display:flex; justify-content:center; gap:12px; }</style>
