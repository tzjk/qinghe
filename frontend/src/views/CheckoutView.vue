<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCart } from '../api/cart'
import { getAddresses } from '../api/address'
import { createOrder } from '../api/order'
import { getMyCoupons } from '../api/coupon'
import AsyncState from '../components/AsyncState.vue'
import PageHeader from '../components/PageHeader.vue'

const route = useRoute(); const router = useRouter()
const loading = ref(false); const submitting = ref(false); const error = ref('')
const addresses = ref([]); const selectedAddressId = ref(null); const remark = ref(''); const shopName = ref(''); const shopId = ref(null); const items = ref([]); const coupons = ref([]); const selectedCouponId = ref(null)
const cartItemIds = computed(() => String(route.query.cartItemIds || '').split(',').map((id) => Number(id)).filter((id) => Number.isInteger(id) && id > 0))
const totalAmount = computed(() => items.value.reduce((sum, item) => sum + Number(item.subtotal || 0), 0))
const selectableCoupons = computed(() => coupons.value.filter((coupon) => coupon.status === 'AVAILABLE' && Number(coupon.shopId) === Number(shopId.value) && Number(coupon.thresholdAmount || 0) <= totalAmount.value && new Date(coupon.useStartTime) <= new Date() && new Date(coupon.expireTime) >= new Date()))
const selectedCoupon = computed(() => selectableCoupons.value.find((coupon) => coupon.id === selectedCouponId.value))
const discountAmount = computed(() => { const coupon = selectedCoupon.value; if (!coupon) return 0; const amount = coupon.couponType === 'DISCOUNT' ? totalAmount.value * (1 - Number(coupon.discountRate || 0)) : Number(coupon.discountAmount || 0); return Math.min(totalAmount.value, Math.round(amount * 100) / 100) })
const payAmount = computed(() => Math.max(0, totalAmount.value - discountAmount.value))
const money = (value) => Number(value || 0).toFixed(2)
const selectedAddress = computed(() => addresses.value.find((item) => item.id === selectedAddressId.value))

async function load() {
  loading.value = true; error.value = ''
  try {
    if (!cartItemIds.value.length) throw new Error('请从购物车选择商品后再结算')
    const [cart, addressList, userCoupons] = await Promise.all([getCart(), getAddresses(), getMyCoupons({ page: 1, size: 100, status: 'AVAILABLE' })])
    const groups = (cart.shopGroups || []).map((group) => ({ ...group, items: group.items.filter((item) => cartItemIds.value.includes(item.cartId)) })).filter((group) => group.items.length)
    if (groups.length !== 1 || groups[0].items.length !== cartItemIds.value.length) throw new Error('购物车商品已变化，请重新选择')
    shopName.value = groups[0].shopName; shopId.value = groups[0].shopId; items.value = groups[0].items; addresses.value = addressList || []; coupons.value = userCoupons.records || []
    selectedAddressId.value = (addresses.value.find((item) => Number(item.isDefault) === 1) || addresses.value[0])?.id || null
  } catch (e) { error.value = e.message || '结算信息加载失败' } finally { loading.value = false }
}
async function submit() {
  if (!selectedAddressId.value) { ElMessage.warning('请先前往地址管理新增校园地址'); return }
  submitting.value = true
  try {
    const result = await createOrder({ cartItemIds: cartItemIds.value, addressId: selectedAddressId.value, userCouponId: selectedCouponId.value || undefined, remark: remark.value || undefined })
    router.replace({ name: 'order-success', params: { orderId: result.orderId }, state: { order: result } })
  } catch (e) { error.value = e.message || '订单提交失败' } finally { submitting.value = false }
}
onMounted(load)
</script>

<template>
  <section class="page-shell"><PageHeader title="结算" description="优惠金额为预估，提交订单后由服务端按当前券、店铺和商品金额重新计算。" eyebrow="CHECKOUT" /><AsyncState :loading="loading" :error="error" :empty="!items.length" empty-text="没有可结算的购物车商品" @retry="load"><template #empty-action><el-button type="primary" @click="router.push('/cart')">返回购物车</el-button></template><div class="checkout-grid"><main><el-card class="ui-card"><template #header>收货地址</template><el-radio-group v-if="addresses.length" v-model="selectedAddressId" class="address-list"><el-radio v-for="address in addresses" :key="address.id" :label="address.id"><strong>{{ address.receiverName }}</strong><span>{{ address.maskedReceiverPhone }}</span><p>{{ address.formattedAddress }}</p></el-radio></el-radio-group><el-empty v-else description="暂无校园地址"><el-button type="primary" @click="router.push('/profile/addresses')">去管理地址</el-button></el-empty></el-card><el-card class="ui-card"><template #header>{{ shopName }}</template><div v-for="item in items" :key="item.cartId" class="checkout-item"><span>{{ item.goodsName }} × {{ item.quantity }}</span><strong>¥{{ money(item.subtotal) }}</strong></div><el-form-item label="订单备注"><el-input v-model="remark" maxlength="255" show-word-limit placeholder="选填，商家可见" /></el-form-item></el-card></main><aside><el-card class="ui-card"><el-form-item label="使用优惠券"><el-select v-model="selectedCouponId" clearable placeholder="不使用优惠券"><el-option v-for="coupon in selectableCoupons" :key="coupon.id" :value="coupon.id" :label="`${coupon.name}（满¥${money(coupon.thresholdAmount)}）`" /></el-select></el-form-item><div class="amount-row"><span>商品原价</span><strong>¥{{ money(totalAmount) }}</strong></div><div class="amount-row"><span>配送费</span><span>¥0.00</span></div><div class="amount-row"><span>优惠金额</span><span>-¥{{ money(discountAmount) }}</span></div><div class="pay-row"><span>实付金额</span><strong>¥{{ money(payAmount) }}</strong></div><el-button type="primary" size="large" :loading="submitting" :disabled="!items.length || !selectedAddressId || submitting" @click="submit">提交订单</el-button></el-card></aside></div></AsyncState></section>
</template>

<style scoped>
.checkout-grid { display:grid; grid-template-columns:minmax(0,1fr) 300px; gap:18px; }.checkout-grid main { display:grid; gap:16px; }.address-list { display:grid; gap:12px; }.address-list :deep(.el-radio) { height:auto; margin-right:0; white-space:normal; }.address-list span { margin-left:8px; color:var(--qh-text-secondary); }.address-list p { margin:7px 0 0; color:var(--qh-text-secondary); line-height:1.6; }.checkout-item,.amount-row,.pay-row { display:flex; justify-content:space-between; gap:12px; padding:11px 0; border-bottom:1px solid var(--qh-border); }.pay-row { margin-top:10px; border:0; font-size:18px; }.pay-row strong { color:#df5a43; font-size:24px; }.el-button { width:100%; margin-top:14px; } @media (max-width:800px) { .checkout-grid { grid-template-columns:1fr; } }
</style>
