<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { acceptAdminOrder, completeAdminOrder, deliverAdminOrder, getAdminOrderDetail, getAdminOrders } from '../api/admin-order'
import { connectOrderWebSocket } from '../utils/order-websocket'

const loading = ref(false); const detailVisible = ref(false); const detail = ref(null); const operating = ref(null)
const query = reactive({ page: 1, size: 10, keyword: '', status: '' }); const result = reactive({ records: [], total: 0 })
const statuses = [['', '全部状态'], ['PENDING_PAY', '待支付'], ['PAID', '已支付'], ['ACCEPTED', '已接单'], ['DELIVERING', '配送中'], ['COMPLETED', '已完成'], ['CANCELLED', '已取消']]
const money = (value) => Number(value || 0).toFixed(2)
const statusRank = { PENDING_PAY: 10, PAID: 20, ACCEPTED: 30, DELIVERING: 40, COMPLETED: 50, CANCELLED: 60 }
let closeWebSocket = () => {}
async function load() { loading.value = true; try { const page = await getAdminOrders({ ...query, keyword: query.keyword || undefined, status: query.status || undefined }); result.records = page.records || []; result.total = page.total || 0 } catch (error) { ElMessage.error(error.message || '订单列表加载失败') } finally { loading.value = false } }
function search() { query.page = 1; load() }
async function showDetail(row) { detailVisible.value = true; try { detail.value = await getAdminOrderDetail(row.orderId) } catch (error) { ElMessage.error(error.message || '订单详情加载失败') } }
async function operate(row, type) { const labels = { accept: '接单', deliver: '开始配送', complete: '完成订单' }; const calls = { accept: acceptAdminOrder, deliver: deliverAdminOrder, complete: completeAdminOrder }; try { await ElMessageBox.confirm(`确认${labels[type]}订单 ${row.orderNo} 吗？`, '订单状态变更', { type: 'warning' }); operating.value = row.orderId; await calls[type](row.orderId); ElMessage.success(`${labels[type]}成功`); await load(); if (detail.value?.orderId === row.orderId) detail.value = await getAdminOrderDetail(row.orderId) } catch (error) { if (error !== 'cancel') ElMessage.error(error.message || '订单操作失败') } finally { operating.value = null } }
function applyAdminMessage(message) {
  const row = result.records.find(item => item.orderId === message.orderId)
  if (row && (statusRank[message.orderStatus] || 0) >= (statusRank[row.status] || 0)) {
    row.status = message.orderStatus
    row.statusName = message.statusText
  }
  if (detail.value?.orderId === message.orderId && (statusRank[message.orderStatus] || 0) >= (statusRank[detail.value.status] || 0)) {
    detail.value.status = message.orderStatus
    detail.value.statusName = message.statusText
  }
  ElMessage.info(message.summary)
  load()
}
onMounted(() => { load(); closeWebSocket = connectOrderWebSocket('admin', applyAdminMessage) })
onBeforeUnmount(() => closeWebSocket())
</script>

<template>
  <section class="admin-order-page"><div class="page-heading"><div><p>ORDER ADMIN</p><h1>订单管理</h1><span>仅能执行当前订单状态允许的固定操作。</span></div></div>
    <el-card class="filter-card" shadow="never"><el-form class="filter-form" @submit.prevent="search"><el-form-item label="订单号"><el-input v-model="query.keyword" clearable placeholder="订单号关键词" @keyup.enter="search" /></el-form-item><el-form-item label="订单状态"><el-select v-model="query.status" clearable><el-option v-for="item in statuses" :key="item[0]" :label="item[1]" :value="item[0]" /></el-select></el-form-item><el-button type="primary" :loading="loading" @click="search">查询</el-button><el-button @click="Object.assign(query,{page:1,keyword:'',status:''});load()">重置</el-button></el-form></el-card>
    <el-card shadow="never"><el-table v-loading="loading" :data="result.records"><el-table-column prop="orderNo" label="订单号" min-width="190" /><el-table-column prop="shopName" label="店铺" min-width="120" /><el-table-column label="用户" min-width="130"><template #default="{ row }">{{ row.userDisplayName || '用户' }}<small>{{ row.maskedUserPhone }}</small></template></el-table-column><el-table-column label="金额" width="130"><template #default="{ row }"><div>原额 ¥{{ money(row.totalAmount) }}</div><strong>实付 ¥{{ money(row.payAmount) }}</strong></template></el-table-column><el-table-column label="状态" width="105"><template #default="{ row }"><el-tag>{{ row.statusName }}</el-tag></template></el-table-column><el-table-column prop="createTime" label="创建时间" min-width="165" /><el-table-column label="操作" fixed="right" width="260"><template #default="{ row }"><el-button text @click="showDetail(row)">详情</el-button><el-button v-if="row.status === 'PAID'" text type="primary" :loading="operating === row.orderId" @click="operate(row,'accept')">接单</el-button><el-button v-if="row.status === 'ACCEPTED'" text type="primary" :loading="operating === row.orderId" @click="operate(row,'deliver')">开始配送</el-button><el-button v-if="row.status === 'DELIVERING'" text type="success" :loading="operating === row.orderId" @click="operate(row,'complete')">完成订单</el-button></template></el-table-column></el-table><el-pagination v-if="result.total" v-model:current-page="query.page" :page-size="query.size" :total="result.total" layout="total, prev, pager, next" @current-change="load" /></el-card>
    <el-dialog v-model="detailVisible" title="订单详情" width="min(760px, calc(100vw - 28px))"><template v-if="detail"><el-descriptions :column="1" border><el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item><el-descriptions-item label="用户">{{ detail.userDisplayName }}（{{ detail.maskedUserPhone }}）</el-descriptions-item><el-descriptions-item label="配送地址">{{ detail.deliveryAddress }}</el-descriptions-item><el-descriptions-item label="状态">{{ detail.statusName }}</el-descriptions-item></el-descriptions><el-table :data="detail.items || []" class="detail-items"><el-table-column prop="goodsName" label="商品" /><el-table-column prop="quantity" label="数量" width="80" /><el-table-column label="小计" width="120"><template #default="{ row }">¥{{ money(row.subtotal) }}</template></el-table-column></el-table></template></el-dialog>
  </section>
</template>

<style scoped>
.admin-order-page{max-width:1440px;margin:0 auto}.page-heading{margin-bottom:20px}.page-heading p{margin:0;color:#5a7db7;font-size:12px;font-weight:700;letter-spacing:.12em}.page-heading h1{margin:4px 0;color:#213758}.page-heading span,small{color:#7e8ea5}.filter-card{margin-bottom:16px}.filter-form{display:flex;flex-wrap:wrap;align-items:flex-end;gap:0 12px}.filter-form .el-input,.filter-form .el-select{width:180px}.detail-items{margin-top:18px}@media(max-width:680px){.filter-form{display:grid;grid-template-columns:1fr}.filter-form .el-input,.filter-form .el-select{width:100%}}
</style>
