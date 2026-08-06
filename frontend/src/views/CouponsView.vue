<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { claimCoupon, getCoupons, getMyCoupons, getSeckillOrderStatus, seckillCoupon } from '../api/coupon'
import PageHeader from '../components/PageHeader.vue'

const active = ref('available'); const loading = ref(false); const claiming = ref(null)
const available = ref([]); const mine = ref([]); const mineStatus = ref(''); let disposed = false
const money = (value) => Number(value || 0).toFixed(2)
const isClaimed = (coupon) => coupon?.claimed === true
const isSeckill = (coupon) => coupon?.couponStatus === 'SECKILL'
const statusOptions = [['', '全部'], ['AVAILABLE', '可使用'], ['LOCKED', '已锁定'], ['USED', '已核销'], ['EXPIRED', '已过期']]
const description = (coupon) => coupon.couponType === 'DISCOUNT' ? `${Number(coupon.discountRate || 0) * 10} 折` : `减 ¥${money(coupon.discountAmount)}`
const wait = (millis) => new Promise((resolve) => window.setTimeout(resolve, millis))
async function loadAvailable() { loading.value = true; try { available.value = (await getCoupons({ page: 1, size: 50 })).records || [] } catch (error) { ElMessage.error(error.message || '可领取优惠券加载失败') } finally { loading.value = false } }
async function loadMine() { loading.value = true; try { mine.value = (await getMyCoupons({ page: 1, size: 50, status: mineStatus.value || undefined })).records || [] } catch (error) { ElMessage.error(error.message || '我的优惠券加载失败') } finally { loading.value = false } }
async function followSeckillOrder(orderId) {
  for (let attempt = 0; attempt < 20 && !disposed; attempt += 1) {
    const result = await getSeckillOrderStatus(orderId)
    if (result.status === 'SUCCESS') { ElMessage.success('抢购成功'); await loadAvailable(); return }
    if (result.status === 'FAILED') { ElMessage.error('抢购未成功，库存已恢复'); await loadAvailable(); return }
    await wait(1000)
  }
  if (!disposed) ElMessage.info('订单仍在排队处理中，请稍后刷新页面查看结果')
}
async function claim(item) {
  if (isClaimed(item) || claiming.value !== null) return
  claiming.value = item.id
  try {
    if (isSeckill(item)) {
      const accepted = await seckillCoupon(item.id)
      ElMessage.info('排队处理中')
      await followSeckillOrder(accepted.orderId)
      return
    }
    const result = await claimCoupon(item.id)
    if (result.claimStatus === 'ALREADY_CLAIMED') { ElMessage.warning(result.message || '该优惠券已领取，请勿重复领取'); await loadAvailable(); return }
    if (result.claimStatus === 'CLAIM_SUCCESS') { item.claimed = true; item.userCouponId = result.userCouponId; item.userCouponStatus = 'AVAILABLE'; ElMessage.success(result.message || '领取成功'); return }
    ElMessage.warning(result.message || '暂时无法领取该优惠券'); await loadAvailable()
  } catch (error) {
    ElMessage.error(error.message || '请先登录后再领取或抢购')
  } finally { claiming.value = null }
}
function changeTab() { active.value === 'available' ? loadAvailable() : loadMine() }
onMounted(loadAvailable); onBeforeUnmount(() => { disposed = true })
</script>

<template>
  <section class="page-shell"><PageHeader title="优惠券" description="领取和抢购均以服务端活动时间、库存与异步订单处理结果为准。" eyebrow="COUPONS" />
    <el-tabs v-model="active" @tab-change="changeTab"><el-tab-pane label="可领取" name="available"><div v-loading="loading" class="coupon-grid"><el-empty v-if="!available.length" description="暂无可领取优惠券" /><el-card v-for="item in available" :key="item.id" class="coupon-card"><h3>{{ item.name }}</h3><strong>{{ description(item) }}</strong><p>满 ¥{{ money(item.thresholdAmount) }} 可用 · 适用店铺 #{{ item.shopId }}</p><small>领取：{{ item.receiveStartTime }} 至 {{ item.receiveEndTime }}</small><small>使用：{{ item.useStartTime }} 至 {{ item.useEndTime }}</small><el-button type="primary" :loading="claiming === item.id" :disabled="isClaimed(item) || claiming !== null" @click="claim(item)">{{ isClaimed(item) ? '已领取' : (isSeckill(item) ? '立即抢购' : '立即领取') }}</el-button></el-card></div></el-tab-pane>
      <el-tab-pane label="我的优惠券" name="mine"><el-radio-group v-model="mineStatus" @change="loadMine"><el-radio-button v-for="item in statusOptions" :key="item[0]" :label="item[0]">{{ item[1] }}</el-radio-button></el-radio-group><div v-loading="loading" class="coupon-grid"><el-empty v-if="!mine.length" description="暂无优惠券" /><el-card v-for="item in mine" :key="item.id" class="coupon-card"><div class="coupon-head"><h3>{{ item.name || '优惠券' }}</h3><el-tag>{{ item.status }}</el-tag></div><strong>{{ description(item) }}</strong><p>满 ¥{{ money(item.thresholdAmount) }} 可用 · 店铺 #{{ item.shopId }}</p><small>有效期至：{{ item.expireTime }}</small></el-card></div></el-tab-pane></el-tabs>
  </section>
</template>
<style scoped>.coupon-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:16px;margin-top:16px}.coupon-card{display:grid;gap:10px}.coupon-card h3,.coupon-card p{margin:0}.coupon-card strong{font-size:24px;color:#df5a43}.coupon-card small{color:var(--qh-text-secondary)}.coupon-head{display:flex;justify-content:space-between;gap:8px;align-items:center}.coupon-card .el-button{justify-self:end}</style>
