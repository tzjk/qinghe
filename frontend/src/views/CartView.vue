<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Delete, Refresh, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearCart, deleteCartItem, getCart, updateCartQuantity, updateCartSelected } from '../api/cart'
import AsyncState from '../components/AsyncState.vue'
import PageHeader from '../components/PageHeader.vue'
import StatusTag from '../components/StatusTag.vue'
import goodsPlaceholder from '../assets/goods-placeholder.svg'

const data = ref({ shopGroups: [], selectedCount: 0, selectedAmount: 0, totalCount: 0 })
const router = useRouter()
const loading = ref(false)
const error = ref('')
const operating = ref({})
const failedImageIds = ref(new Set())
const selectedShopCount = computed(() => data.value.shopGroups.filter((group) => group.items.some((item) => item.selected)).length)
const money = (value) => Number(value || 0).toFixed(2)
const canOperate = (item) => item.saleStatus === 'ON_SALE' && Number(item.stock) > 0 && Number(item.quantity) <= Number(item.stock)
const maxQuantity = (item) => Math.max(1, Math.min(99, Number(item.stock || 0)))
const cartImageSrc = (item) => failedImageIds.value.has(item.cartId) ? goodsPlaceholder : (item.goodsImage || goodsPlaceholder)
const useImagePlaceholder = (cartId) => { failedImageIds.value = new Set(failedImageIds.value).add(cartId) }

async function load() {
  loading.value = true
  error.value = ''
  failedImageIds.value = new Set()
  try {
    data.value = await getCart()
  } catch (e) {
    error.value = e.message || '购物车加载失败'
  } finally {
    loading.value = false
  }
}

async function run(key, action) {
  operating.value[key] = true
  try {
    await action()
    await load()
  } catch (e) {
    error.value = e.message || '操作失败'
    await load()
  } finally {
    operating.value[key] = false
  }
}

function changeQuantity(item, quantity) {
  if (!canOperate(item) || !quantity) return
  run(`q-${item.cartId}`, async () => {
    await updateCartQuantity(item.cartId, { quantity })
    ElMessage.success('数量已更新')
  })
}

function changeSelected(item, selected) {
  if (!canOperate(item)) return
  run(`s-${item.cartId}`, async () => {
    await updateCartSelected(item.cartId, { selected })
    ElMessage.success('选中状态已更新')
  })
}

async function remove(item) {
  try {
    await ElMessageBox.confirm(`确认删除“${item.goodsName}”吗？`, '删除商品', { type: 'warning' })
    await run(`d-${item.cartId}`, async () => {
      await deleteCartItem(item.cartId)
      ElMessage.success('商品已删除')
    })
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') error.value = e.message || '删除失败'
  }
}

async function removeAll() {
  try {
    await ElMessageBox.confirm('确认清空当前购物车吗？', '清空购物车', { type: 'warning' })
    await run('clear', async () => {
      await clearCart()
      ElMessage.success('购物车已清空')
    })
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') error.value = e.message || '清空失败'
  }
}

function checkout() {
  if (!data.value.selectedCount) return
  if (selectedShopCount.value > 1) {
    ElMessage.warning('一次只能结算一个商铺')
    return
  }
  const group = data.value.shopGroups.find((item) => item.items.some((cartItem) => cartItem.selected))
  const cartItemIds = group.items.filter((item) => item.selected).map((item) => item.cartId)
  router.push({ name: 'checkout', query: { cartItemIds: cartItemIds.join(',') } })
}

onMounted(load)
</script>

<template>
  <section class="page-shell">
    <PageHeader title="购物车" description="商品价格、库存和选中金额均以服务端最新数据为准。" eyebrow="MY CART">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="danger" plain :icon="Delete" :disabled="!data.totalCount" :loading="operating.clear" @click="removeAll">清空</el-button>
      </template>
    </PageHeader>
    <AsyncState :loading="loading" :error="error" :empty="!data.shopGroups?.length" empty-text="购物车为空" :skeleton-rows="9" @retry="load">
      <template #empty-action>
        <el-button type="primary" :icon="ShoppingCart" @click="$router.push('/shops')">去逛商铺</el-button>
      </template>
      <div class="cart-layout">
        <div class="cart-groups">
          <el-card v-for="group in data.shopGroups" :key="group.shopId" class="ui-card shop-group">
            <template #header>
              <div class="group-header"><strong>{{ group.shopName }}</strong><span>{{ group.items.length }} 件商品</span></div>
            </template>
            <article v-for="item in group.items" :key="item.cartId" class="cart-item">
              <el-checkbox :model-value="item.selected" :disabled="!canOperate(item) || operating[`s-${item.cartId}`]" @update:model-value="changeSelected(item, $event)" />
              <el-image class="cart-image" :src="cartImageSrc(item)" fit="cover" :alt="`${item.goodsName} 商品图片`" @error="useImagePlaceholder(item.cartId)">
                <template #error><img class="cart-image-placeholder" :src="goodsPlaceholder" :alt="`${item.goodsName} 商品图片占位`" /></template>
              </el-image>
              <div class="item-main">
                <div class="item-title"><strong>{{ item.goodsName }}</strong><StatusTag :status="item.saleStatus" /></div>
                <p class="meta">库存 {{ item.stock }}<span v-if="!canOperate(item)"> · 当前不可购买</span></p>
                <p class="item-price">￥{{ money(item.price) }}</p>
              </div>
              <div class="item-quantity">
                <el-input-number :model-value="item.quantity" :min="1" :max="maxQuantity(item)" :disabled="!canOperate(item) || operating[`q-${item.cartId}`]" @change="changeQuantity(item, $event)" />
                <span>小计 ￥{{ money(item.subtotal) }}</span>
              </div>
              <el-button text type="danger" :loading="operating[`d-${item.cartId}`]" @click="remove(item)">删除</el-button>
            </article>
          </el-card>
        </div>
        <aside class="summary-panel">
          <el-card class="ui-card">
            <p class="summary-label">已选商品</p><p><strong>{{ data.selectedCount }}</strong> 件</p>
            <p class="summary-label">合计金额</p><p class="summary-price">￥{{ money(data.selectedAmount) }}</p>
            <el-button type="primary" size="large" :disabled="!data.selectedCount" @click="checkout">去结算</el-button>
            <p class="summary-hint">订单功能建设中，当前不会创建订单或支付记录。</p>
          </el-card>
        </aside>
      </div>
    </AsyncState>
  </section>
</template>

<style scoped>
.cart-layout { display: grid; grid-template-columns: minmax(0, 1fr) 292px; gap: 18px; align-items: start; }
.cart-groups { display: grid; gap: 16px; }
.group-header { display: flex; justify-content: space-between; align-items: center; }
.group-header span { color: var(--qh-text-secondary); font-size: 13px; }
.cart-item { display: grid; grid-template-columns: auto 82px minmax(0, 1fr) 145px auto; gap: 14px; align-items: center; padding: 16px 0; border-bottom: 1px solid var(--qh-border); }
.cart-item:last-child { padding-bottom: 0; border-bottom: 0; }
.cart-image { width: 82px; height: 82px; overflow: hidden; border-radius: 10px; font-size: 12px; }
.cart-image-placeholder { display: block; width: 100%; height: 100%; object-fit: cover; }
.item-title { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; }
.item-main { min-width: 0; }
.item-main strong { line-height: 1.5; }
.item-main p { margin: 6px 0 0; }
.item-price { color: #df5a43; font-size: 17px; font-weight: 700; }
.item-quantity { display: grid; gap: 8px; justify-items: end; color: var(--qh-text-secondary); font-size: 13px; }
.summary-panel { position: sticky; top: 16px; }
.summary-panel .el-button { width: 100%; margin-top: 10px; }
.summary-label { margin: 0; color: var(--qh-text-secondary); font-size: 13px; }
.summary-label + p { margin: 6px 0 18px; }
.summary-price { margin: 6px 0 14px; color: #df5a43; font-size: 27px; font-weight: 800; }
.summary-hint { margin: 14px 0 0; color: var(--qh-text-secondary); font-size: 12px; line-height: 1.6; }
@media (max-width: 880px) { .cart-layout { grid-template-columns: 1fr; }.summary-panel { position: static; }.cart-item { grid-template-columns: auto 72px minmax(0, 1fr) auto; }.item-quantity { grid-column: 3 / 5; justify-items: start; } }
@media (max-width: 540px) { .cart-item { grid-template-columns: auto 64px minmax(0, 1fr); gap: 10px; }.cart-image { width: 64px; height: 64px; }.cart-item > .el-button { grid-column: 3; justify-self: start; }.item-quantity { grid-column: 3; } }
</style>
