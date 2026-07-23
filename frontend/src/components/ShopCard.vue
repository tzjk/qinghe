<script setup>
import { Location, Shop } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const props = defineProps({ shop: { type: Object, required: true } })
const router = useRouter()
function openDetail() { router.push(`/shops/${props.shop.id}`) }
</script>

<template>
  <article class="shop-card" @click="openDetail">
    <el-image v-if="shop.coverImage" class="shop-cover" :src="shop.coverImage" fit="cover"><template #error><div class="media-placeholder"><el-icon><Shop /></el-icon></div></template></el-image>
    <div v-else class="shop-cover media-placeholder"><el-icon><Shop /></el-icon></div>
    <div class="shop-body">
      <div class="shop-title"><h3>{{ shop.name }}</h3><span v-if="shop.score" class="score">★ {{ shop.score }}</span></div>
      <p v-if="shop.categoryName" class="meta">{{ shop.categoryName }}</p>
      <p v-if="shop.address" class="meta address"><el-icon><Location /></el-icon>{{ shop.address }}</p>
      <el-button type="primary" plain @click.stop="openDetail">进入商铺</el-button>
    </div>
  </article>
</template>

<style scoped>
.shop-card { overflow: hidden; border: 1px solid var(--qh-border); border-radius: var(--qh-radius-lg); background: var(--qh-surface); box-shadow: var(--qh-shadow-card); cursor: pointer; transition: transform .18s ease, box-shadow .18s ease; }
.shop-card:hover { transform: translateY(-3px); box-shadow: 0 12px 28px rgba(37, 65, 109, .12); }
.shop-cover { display: block; width: 100%; height: 164px; font-size: 32px; }
.shop-body { padding: 16px; }
.shop-title { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
h3 { margin: 0; font-size: 17px; line-height: 1.45; }
.score { color: #d98b2b; font-size: 13px; white-space: nowrap; }
.address { display: flex; align-items: center; gap: 5px; min-height: 44px; }
.shop-body .el-button { width: 100%; margin-top: 6px; }
</style>
