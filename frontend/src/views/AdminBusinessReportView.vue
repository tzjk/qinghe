<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getBusinessOverview, getBusinessTrend, getCouponUsageSummary, getGoodsSalesRanking, getShopSalesRanking } from '../api/admin-report'

const loading = ref(false)
const overview = ref(null)
const trend = ref([])
const shops = ref([])
const goods = ref([])
const coupons = ref([])
const range = ref([])
const params = computed(() => range.value?.length === 2 ? { startDate: range.value[0], endDate: range.value[1], top: 10 } : { top: 10 })
const money = (value) => `¥${Number(value || 0).toFixed(2)}`
const points = computed(() => {
  const items = trend.value
  if (!items.length) return ''
  const maximum = Math.max(...items.map(item => Number(item.salesAmount || 0)), 1)
  return items.map((item, index) => `${items.length === 1 ? 50 : index * 100 / (items.length - 1)},${92 - Number(item.salesAmount || 0) * 82 / maximum}`).join(' ')
})
async function load() {
  loading.value = true
  try {
    const [overviewData, trendData, shopData, goodsData, couponData] = await Promise.all([
      getBusinessOverview(), getBusinessTrend(params.value), getShopSalesRanking(params.value), getGoodsSalesRanking(params.value), getCouponUsageSummary(params.value)
    ])
    overview.value = overviewData; trend.value = trendData; shops.value = shopData; goods.value = goodsData; coupons.value = couponData
  } catch (error) { ElMessage.error(error?.message || '营业报表加载失败，请稍后重试') } finally { loading.value = false }
}
function reset() { range.value = []; load() }
onMounted(load)
</script>

<template>
  <section v-loading="loading" class="report-page">
    <div class="report-head"><div><h2>营业报表</h2><p>金额、趋势与排行均由服务端实时汇总。</p></div><div class="report-actions"><el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"/><el-button type="primary" @click="load">查询</el-button><el-button @click="reset">最近7天</el-button></div></div>
    <el-alert title="营业额仅统计已支付、已接单、配送中和已完成订单；待支付、已取消订单不计入营业额。" type="info" :closable="false" show-icon />
    <div class="cards" v-if="overview"><el-card><span>今日营业额</span><strong>{{ money(overview.salesAmount) }}</strong></el-card><el-card><span>今日订单数</span><strong>{{ overview.orderCount }}</strong></el-card><el-card><span>今日支付订单</span><strong>{{ overview.paidOrderCount }}</strong></el-card><el-card><span>今日完成订单</span><strong>{{ overview.completedOrderCount }}</strong></el-card><el-card><span>今日取消订单</span><strong>{{ overview.cancelledOrderCount }}</strong></el-card><el-card><span>今日优惠金额</span><strong>{{ money(overview.discountAmount) }}</strong></el-card><el-card><span>今日新增用户</span><strong>{{ overview.newUserCount }}</strong></el-card><el-card><span>今日有订单店铺</span><strong>{{ overview.activeShopCount }}</strong></el-card></div>
    <el-card class="panel"><template #header>待处理订单（分状态）</template><el-space wrap><el-tag>待支付 {{ overview?.pendingPayOrderCount || 0 }}</el-tag><el-tag type="warning">已支付待接单 {{ overview?.paidPendingOrderCount || 0 }}</el-tag><el-tag type="warning">已接单 {{ overview?.acceptedOrderCount || 0 }}</el-tag><el-tag type="success">配送中 {{ overview?.deliveringOrderCount || 0 }}</el-tag></el-space></el-card>
    <el-card class="panel"><template #header>日期范围营业趋势</template><el-empty v-if="!trend.length" description="所选日期暂无数据"/><div v-else class="trend"><svg viewBox="0 0 100 100" preserveAspectRatio="none" aria-label="营业额趋势图"><polyline :points="points" fill="none" stroke="#409eff" stroke-width="2" vector-effect="non-scaling-stroke"/></svg><div class="trend-labels"><span v-for="item in trend" :key="item.reportDate">{{ item.reportDate.slice(5) }}<b>{{ money(item.salesAmount) }}</b></span></div></div></el-card>
    <div class="rank-grid"><el-card><template #header>店铺销售排行</template><el-empty v-if="!shops.length" description="暂无有效支付订单"/><el-table v-else :data="shops" size="small"><el-table-column type="index" label="#" width="48"/><el-table-column prop="shopName" label="店铺"/><el-table-column prop="paidOrderCount" label="支付订单" width="90"/><el-table-column label="销售额" width="110"><template #default="{ row }">{{ money(row.salesAmount) }}</template></el-table-column></el-table></el-card><el-card><template #header>商品销量排行</template><el-empty v-if="!goods.length" description="暂无有效支付订单"/><el-table v-else :data="goods" size="small"><el-table-column type="index" label="#" width="48"/><el-table-column prop="goodsName" label="商品"/><el-table-column prop="salesQuantity" label="销量" width="70"/><el-table-column label="销售额" width="110"><template #default="{ row }">{{ money(row.salesAmount) }}</template></el-table-column></el-table></el-card></div>
    <el-card class="panel"><template #header>优惠券使用统计</template><el-empty v-if="!coupons.length" description="所选日期暂无已核销优惠券"/><el-table v-else :data="coupons" size="small"><el-table-column type="index" label="#" width="60"/><el-table-column prop="couponName" label="优惠券"/><el-table-column prop="usedCount" label="使用次数" width="120"/><el-table-column label="优惠金额" width="140"><template #default="{ row }">{{ money(row.discountAmount) }}</template></el-table-column></el-table></el-card>
  </section>
</template>

<style scoped>
.report-page{display:grid;gap:16px}.report-head{display:flex;justify-content:space-between;gap:16px;align-items:center}.report-head h2{margin:0}.report-head p{margin:6px 0 0;color:#909399}.report-actions{display:flex;gap:8px;flex-wrap:wrap}.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(165px,1fr));gap:12px}.cards span{display:block;color:#909399;font-size:13px}.cards strong{display:block;margin-top:8px;font-size:24px;color:#303133}.panel{margin:0}.trend{height:230px;padding:12px 4px}.trend svg{width:100%;height:180px;background:linear-gradient(#f5f9ff 1px,transparent 1px);background-size:100% 25%}.trend-labels{display:flex;justify-content:space-between;gap:4px;font-size:12px;color:#909399;overflow:auto}.trend-labels span{min-width:58px;text-align:center}.trend-labels b{display:block;color:#606266;font-weight:500}.rank-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}@media(max-width:900px){.report-head{align-items:flex-start;flex-direction:column}.rank-grid{grid-template-columns:1fr}}
</style>
