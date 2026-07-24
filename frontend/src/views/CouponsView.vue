<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { claimCoupon, getCoupons, getMyCoupons } from '../api/coupon'
import PageHeader from '../components/PageHeader.vue'

const active = ref('available'); const loading = ref(false); const claiming = ref(null)
const available = ref([]); const mine = ref([]); const mineStatus = ref('')
const money = (value) => Number(value || 0).toFixed(2)
const isClaimed = (coupon) => coupon?.claimed === true
const statusOptions = [['', '全部'], ['AVAILABLE', '可使用'], ['LOCKED', '已锁定'], ['USED', '已核销'], ['EXPIRED', '已过期']]
const description = (coupon) => coupon.couponType === 'DISCOUNT' ? `${Number(coupon.discountRate || 0) * 10} 折` : `减 ¥${money(coupon.discountAmount)}`
async function loadAvailable() { loading.value = true; try { available.value = (await getCoupons({ page: 1, size: 50 })).records || [] } catch (error) { ElMessage.error(error.message || '可领取优惠券加载失败') } finally { loading.value = false } }
async function loadMine() { loading.value = true; try { mine.value = (await getMyCoupons({ page: 1, size: 50, status: mineStatus.value || undefined })).records || [] } catch (error) { ElMessage.error(error.message || '我的优惠券加载失败') } finally { loading.value = false } }
async function claim(item) {
  if (isClaimed(item)) return
  claiming.value = item.id
  try {
    const result = await claimCoupon(item.id)
    if (result.claimStatus === 'ALREADY_CLAIMED') {
      ElMessage.warning(result.message || '该优惠券已领取，请勿重复领取')
      await loadAvailable()
      return
    }
    if (result.claimStatus === 'CLAIM_SUCCESS') {
      item.claimed = true
      item.userCouponId = result.userCouponId
      item.userCouponStatus = 'AVAILABLE'
      item.availableStock = Math.max(0, Number(item.availableStock || 0) - 1)
      ElMessage.success(result.message || '领取成功')
      return
    }
    ElMessage.warning(result.message || '暂时无法领取该优惠券')
    await loadAvailable()
  } catch (error) { ElMessage.error(error.message || '领取失败') } finally { claiming.value = null }
}
function changeTab() { active.value === 'available' ? loadAvailable() : loadMine() }
onMounted(async () => { await loadAvailable(); if (active.value === 'mine') await loadMine() })
</script>

<template>
  <section class="page-shell"><PageHeader title="优惠券" description="领取与使用均以服务端时间、库存和订单金额校验为准。" eyebrow="COUPONS" />
    <el-tabs v-model="active" @tab-change="changeTab"><el-tab-pane label="可领取" name="available"><div v-loading="loading" class="coupon-grid"><el-empty v-if="!available.length" description="暂无可领取优惠券" /><el-card v-for="item in available" :key="item.id" class="coupon-card"><h3>{{ item.name }}</h3><strong>{{ description(item) }}</strong><p>满 ¥{{ money(item.thresholdAmount) }} 可用 · 适用店铺 #{{ item.shopId }}</p><small>领取：{{ item.receiveStartTime }} 至 {{ item.receiveEndTime }}</small><small>使用：{{ item.useStartTime }} 至 {{ item.useEndTime }}</small><el-button type="primary" :loading="claiming === item.id" :disabled="isClaimed(item) || claiming === item.id" @click="claim(item)">{{ isClaimed(item) ? '已领取' : '立即领取' }}</el-button></el-card></div></el-tab-pane>
      <el-tab-pane label="我的优惠券" name="mine"><el-radio-group v-model="mineStatus" @change="loadMine"><el-radio-button v-for="item in statusOptions" :key="item[0]" :label="item[0]">{{ item[1] }}</el-radio-button></el-radio-group><div v-loading="loading" class="coupon-grid"><el-empty v-if="!mine.length" description="暂无优惠券" /><el-card v-for="item in mine" :key="item.id" class="coupon-card"><div class="coupon-head"><h3>{{ item.name || '优惠券' }}</h3><el-tag>{{ item.status }}</el-tag></div><strong>{{ description(item) }}</strong><p>满 ¥{{ money(item.thresholdAmount) }} 可用 · 店铺 #{{ item.shopId }}</p><small>有效期至：{{ item.expireTime }}</small></el-card></div></el-tab-pane></el-tabs>
  </section>
</template>
<style scoped>.coupon-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:16px;margin-top:16px}.coupon-card{display:grid;gap:10px}.coupon-card h3,.coupon-card p{margin:0}.coupon-card strong{font-size:24px;color:#df5a43}.coupon-card small{color:var(--qh-text-secondary)}.coupon-head{display:flex;justify-content:space-between;gap:8px;align-items:center}.coupon-card .el-button{justify-self:end}</style>
