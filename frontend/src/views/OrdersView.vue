<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, getOrderDetail, getOrders, simulateOrderPayment } from '../api/order'
import PageHeader from '../components/PageHeader.vue'

const loading = ref(false); const detailVisible = ref(false); const detailLoading = ref(false); const operating = ref(null)
const query = reactive({ page: 1, size: 10, status: '' }); const result = reactive({ records: [], total: 0 }); const detail = ref(null)
const statusOptions = [['', '全部状态'], ['PENDING_PAY', '待支付'], ['PAID', '已支付'], ['ACCEPTED', '已接单'], ['DELIVERING', '配送中'], ['COMPLETED', '已完成'], ['CANCELLED', '已取消']]
const money = (value) => Number(value || 0).toFixed(2)

async function load() { loading.value = true; try { const page = await getOrders({ ...query, status: query.status || undefined }); result.records = page.records || []; result.total = page.total || 0 } catch (error) { ElMessage.error(error.message || '订单列表加载失败') } finally { loading.value = false } }
function search() { query.page = 1; load() }
async function openDetail(row) { detailVisible.value = true; detailLoading.value = true; detail.value = null; try { detail.value = await getOrderDetail(row.orderId) } catch (error) { ElMessage.error(error.message || '订单详情加载失败') } finally { detailLoading.value = false } }
async function pay(row) { try { await ElMessageBox.confirm(`确认以订单实付金额 ¥${money(row.payAmount)} 完成模拟支付吗？`, '模拟支付确认', { type: 'warning', confirmButtonText: '确认支付' }); operating.value = row.orderId; await simulateOrderPayment(row.orderId); ElMessage.success('模拟支付成功'); await load(); if (detail.value?.orderId === row.orderId) detail.value = await getOrderDetail(row.orderId) } catch (error) { if (error !== 'cancel') ElMessage.error(error.message || '模拟支付失败') } finally { operating.value = null } }
async function cancel(row) { try { await ElMessageBox.confirm('确认取消该待支付订单吗？已占用的商品库存将由服务端恢复，购物车不会恢复。', '取消订单', { type: 'warning', confirmButtonText: '确认取消' }); operating.value = row.orderId; await cancelOrder(row.orderId); ElMessage.success('订单已取消'); await load(); if (detail.value?.orderId === row.orderId) detail.value = await getOrderDetail(row.orderId) } catch (error) { if (error !== 'cancel') ElMessage.error(error.message || '取消订单失败') } finally { operating.value = null } }
onMounted(load)
</script>

<template>
  <section class="page-shell"><PageHeader title="我的订单" description="查看订单进度；待支付订单可在支付期限内完成模拟支付或主动取消。" eyebrow="MY ORDERS" />
    <el-card class="ui-card filter"><el-radio-group v-model="query.status" @change="search"><el-radio-button v-for="item in statusOptions" :key="item[0]" :label="item[0]">{{ item[1] }}</el-radio-button></el-radio-group></el-card>
    <el-empty v-if="!loading && !result.records.length" description="暂无订单记录" />
    <div v-loading="loading" class="order-list"><el-card v-for="row in result.records" :key="row.orderId" class="order-card ui-card" shadow="never"><div class="order-head"><span>订单号：{{ row.orderNo }}</span><el-tag :type="row.status === 'CANCELLED' ? 'info' : row.status === 'COMPLETED' ? 'success' : 'warning'">{{ row.statusName }}</el-tag></div><div class="order-body"><div><strong>{{ row.shopName || '店铺信息已归档' }}</strong><p>{{ (row.items || []).map(item => `${item.goodsName} × ${item.quantity}`).join('；') || '商品明细' }}</p><small>创建时间：{{ row.createTime }}</small><small v-if="row.status === 'PENDING_PAY'">支付截止：{{ row.payExpireTime || '以服务端校验为准' }}</small></div><div class="amount"><span>商品原额 ¥{{ money(row.totalAmount) }}</span><strong>实付 ¥{{ money(row.payAmount) }}</strong></div></div><div class="actions"><el-button text @click="openDetail(row)">查看详情</el-button><el-button v-if="row.status === 'PENDING_PAY'" type="primary" :loading="operating === row.orderId" @click="pay(row)">模拟支付</el-button><el-button v-if="row.status === 'PENDING_PAY'" type="danger" plain :loading="operating === row.orderId" @click="cancel(row)">取消订单</el-button></div></el-card></div>
    <el-pagination v-if="result.total" v-model:current-page="query.page" :page-size="query.size" :total="result.total" layout="total, prev, pager, next" @current-change="load" />
    <el-dialog v-model="detailVisible" title="订单详情" width="min(760px, calc(100vw - 28px))"><div v-loading="detailLoading" v-if="detail"><el-descriptions :column="1" border><el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item><el-descriptions-item label="状态">{{ detail.statusName }}</el-descriptions-item><el-descriptions-item label="配送地址">{{ detail.deliveryAddress }}</el-descriptions-item><el-descriptions-item label="支付截止">{{ detail.payExpireTime || '-' }}</el-descriptions-item><el-descriptions-item label="实付金额">¥{{ money(detail.payAmount) }}</el-descriptions-item></el-descriptions><el-table :data="detail.items || []" class="detail-items"><el-table-column prop="goodsName" label="商品" /><el-table-column prop="quantity" label="数量" width="80" /><el-table-column label="小计" width="120"><template #default="{ row }">¥{{ money(row.subtotal) }}</template></el-table-column></el-table></div></el-dialog>
  </section>
</template>

<style scoped>
.filter{margin-bottom:16px}.order-list{display:grid;gap:14px}.order-card{border-color:var(--qh-border)}.order-head,.order-body,.actions{display:flex;justify-content:space-between;gap:16px}.order-head{padding-bottom:12px;border-bottom:1px solid var(--qh-border);color:var(--qh-text-secondary)}.order-body{padding:16px 0}.order-body p{margin:8px 0;color:var(--qh-text-secondary)}small{display:block;margin-top:5px;color:var(--qh-text-secondary)}.amount{display:grid;align-content:center;justify-items:end;gap:8px}.amount strong{font-size:18px;color:#df5a43}.actions{justify-content:flex-end}.detail-items{margin-top:18px}@media(max-width:680px){.order-body{display:grid}.amount{justify-items:start}.filter :deep(.el-radio-group){display:flex;flex-wrap:wrap}.actions{flex-wrap:wrap}}
</style>
