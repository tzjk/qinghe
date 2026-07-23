<script setup>
import { Box } from '@element-plus/icons-vue'
import goodsPlaceholder from '../assets/goods-placeholder.svg'
defineProps({ goods: { type: Object, required: true } })
</script>

<template>
  <article class="goods-card">
    <el-image class="goods-cover" :src="goods.coverImage || goodsPlaceholder" fit="contain"><template #error><div class="media-placeholder"><el-icon><Box /></el-icon></div></template></el-image>
    <div class="goods-body"><h3>{{ goods.name }}</h3><p class="description">{{ goods.description || '店内人气商品，欢迎加入购物车。' }}</p><div class="goods-bottom"><div><strong>￥{{ Number(goods.price || 0).toFixed(2) }}</strong><span :class="{ soldout: Number(goods.stock) <= 0 }">{{ Number(goods.stock) <= 0 ? '售罄' : `库存 ${goods.stock}` }}</span></div><slot name="actions" /></div></div>
  </article>
</template>

<style scoped>
.goods-card{overflow:hidden;border:1px solid var(--qh-border);border-radius:12px;background:#fff;box-shadow:0 5px 16px rgba(29,55,92,.06)}.goods-cover{display:block;width:100%;aspect-ratio:1;background:#f6f8fb;padding:10px;box-sizing:border-box}.goods-body{padding:12px}.goods-body h3{margin:0;overflow:hidden;font-size:16px;line-height:1.45;text-overflow:ellipsis;white-space:nowrap}.description{display:-webkit-box;min-height:38px;margin:7px 0;color:var(--qh-text-secondary);font-size:13px;line-height:1.45;overflow:hidden;-webkit-box-orient:vertical;-webkit-line-clamp:2}.goods-bottom{display:flex;align-items:flex-end;justify-content:space-between;gap:8px}.goods-bottom>div{display:grid;gap:3px}.goods-bottom strong{color:#df5a43;font-size:18px}.goods-bottom span{color:#718096;font-size:12px}.goods-bottom .soldout{color:#c2410c}.goods-bottom :deep(.el-button){flex:none}
</style>
