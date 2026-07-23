<script setup>
import { onMounted, ref } from 'vue'
import { ArrowRight, Compass, Menu, Tickets } from '@element-plus/icons-vue'
import { getHomeSummary } from '../api/home'
import AsyncState from '../components/AsyncState.vue'
import GoodsCard from '../components/GoodsCard.vue'
import PageHeader from '../components/PageHeader.vue'
import ShopCard from '../components/ShopCard.vue'

const summary = ref(null)
const loading = ref(true)
const error = ref('')
const price = (value) => Number(value || 0).toFixed(2)
async function load() { loading.value = true; error.value = ''; try { summary.value = await getHomeSummary() } catch (e) { error.value = e.message || '首页数据加载失败' } finally { loading.value = false } }
onMounted(load)
</script>

<template>
  <section class="page-shell">
    <PageHeader title="首页" description="浏览校园周边服务、商品与探店内容。" eyebrow="CAMPUS LIFE">
      <template #actions><el-button :icon="ArrowRight" plain @click="$router.push('/shops')">浏览商铺</el-button><el-button :loading="loading" @click="load">刷新</el-button></template>
    </PageHeader>
    <AsyncState :loading="loading" :error="error" :empty="!summary" empty-text="首页暂无可展示内容" :skeleton-rows="10" @retry="load">
      <section class="campus-hero">
        <div><p class="hero-kicker">青禾校园生活</p><h2>把校园日常，安排得更从容。</h2><p>从身边商铺到热门好物，用真实服务信息帮你快速找到所需。</p><el-button type="primary" @click="$router.push('/shops')">发现校园商铺</el-button></div>
        <div class="hero-banners"><template v-if="summary.banners?.length"><router-link v-for="item in summary.banners.slice(0, 2)" :key="item.id" class="banner-item" :to="`/shops/${item.id}`"><el-image v-if="item.coverImage" :src="item.coverImage" fit="cover" /><div v-else class="banner-fallback">推荐商铺</div><span>{{ item.name }}</span></router-link></template><div v-else class="banner-empty"><el-icon><Compass /></el-icon><span>探索校园周边</span></div></div>
      </section>

      <section><div class="section-heading"><div><h2>快捷分类</h2><p>按兴趣进入相关商铺和服务</p></div></div><div class="category-grid"><router-link v-for="item in summary.categories || []" :key="item.id" class="category-item" :to="{ path: '/shops', query: { categoryId: item.id } }"><el-avatar :size="42" :src="item.iconUrl"><el-icon><Menu /></el-icon></el-avatar><div><strong>{{ item.name }}</strong><span>查看相关商铺</span></div></router-link><el-empty v-if="!summary.categories?.length" description="暂无分类" /></div></section>

      <section><div class="section-heading"><div><h2>推荐商铺</h2><p>来自首页聚合接口的推荐内容</p></div><el-button text type="primary" @click="$router.push('/shops')">查看全部</el-button></div><div v-if="summary.recommendedShops?.length" class="content-grid"><ShopCard v-for="item in summary.recommendedShops" :key="item.id" :shop="item" /></div><div v-else class="state-panel"><el-empty description="暂无推荐商铺" /></div></section>

      <section><div class="section-heading"><div><h2>热门商品</h2><p>当前在售商品与实时价格</p></div></div><div v-if="summary.hotGoods?.length" class="content-grid"><GoodsCard v-for="item in summary.hotGoods" :key="item.id" :goods="item" /></div><div v-else class="state-panel"><el-empty description="暂无热门商品" /></div></section>

      <section class="home-bottom-grid"><el-card class="ui-card"><template #header><div class="card-title"><span><el-icon><Tickets /></el-icon> 优惠券</span><span class="meta">真实可领内容</span></div></template><div v-if="summary.availableCoupons?.length" class="coupon-list"><div v-for="item in summary.availableCoupons" :key="item.id" class="coupon-item"><strong>{{ item.name || item.title }}</strong><span v-if="item.minAmount !== undefined">满 ¥{{ price(item.minAmount) }} 可用</span><span v-else-if="item.description">{{ item.description }}</span></div></div><el-empty v-else description="暂无可领取优惠券" /></el-card><el-card class="ui-card"><template #header><div class="card-title"><span><el-icon><Compass /></el-icon> 探店推荐</span><el-button text type="primary" @click="$router.push('/blogs')">进入探店</el-button></div></template><div v-if="summary.featuredBlogs?.length" class="blog-list"><div v-for="item in summary.featuredBlogs" :key="item.id" class="blog-item"><el-image v-if="item.coverImage" :src="item.coverImage" fit="cover" /><div><strong>{{ item.title || item.name }}</strong><p v-if="item.content">{{ item.content }}</p></div></div></div><el-empty v-else description="暂无探店推荐" /></el-card></section>
    </AsyncState>
  </section>
</template>

<style scoped>
.campus-hero { display: grid; grid-template-columns: minmax(0, 1.3fr) minmax(280px, .7fr); gap: 28px; overflow: hidden; padding: 34px; border-radius: var(--qh-radius-lg); color: #fff; background: linear-gradient(120deg, #2e64bd, #4d89df); box-shadow: 0 12px 28px rgba(47, 96, 180, .18); }
.hero-kicker { margin: 0 0 12px; color: #dceaff; font-size: 13px; font-weight: 700; letter-spacing: .12em; }
.campus-hero h2 { max-width: 520px; margin: 0; font-size: clamp(28px, 4vw, 42px); line-height: 1.28; }
.campus-hero > div > p:not(.hero-kicker) { max-width: 540px; margin: 16px 0 22px; color: #e5efff; line-height: 1.8; }
.campus-hero .el-button { --el-button-bg-color: #fff; --el-button-border-color: #fff; --el-button-text-color: var(--qh-primary-dark); --el-button-hover-bg-color: #edf4ff; --el-button-hover-border-color: #edf4ff; }
.hero-banners { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; align-items: stretch; }
.banner-item { position: relative; display: block; min-height: 180px; overflow: hidden; border-radius: 12px; background: rgba(255,255,255,.15); }
.banner-item .el-image { width: 100%; height: 100%; min-height: 180px; }
.banner-item span { position: absolute; right: 0; bottom: 0; left: 0; padding: 26px 12px 12px; background: linear-gradient(transparent, rgba(19, 43, 82, .75)); font-size: 14px; font-weight: 700; }
.banner-fallback, .banner-empty { display: grid; place-items: center; min-height: 180px; color: #dceaff; text-align: center; }
.banner-empty { grid-column: 1 / -1; gap: 8px; border: 1px dashed rgba(255,255,255,.38); border-radius: 12px; font-size: 15px; }
.banner-empty .el-icon { font-size: 30px; }
.category-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
.category-item { display: flex; align-items: center; gap: 12px; min-height: 78px; padding: 14px; border: 1px solid var(--qh-border); border-radius: var(--qh-radius-md); background: #fff; transition: border-color .16s ease, transform .16s ease; }
.category-item:hover { border-color: #bcd2f5; transform: translateY(-2px); }
.category-item strong, .category-item span { display: block; }.category-item span { margin-top: 5px; color: var(--qh-text-secondary); font-size: 13px; }
.home-bottom-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 36px; }.card-title { display: flex; justify-content: space-between; align-items: center; gap: 12px; font-weight: 700; }.card-title span:first-child { display: inline-flex; align-items: center; gap: 7px; }.coupon-list, .blog-list { display: grid; gap: 12px; }.coupon-item { display: flex; justify-content: space-between; gap: 12px; padding: 12px; border-radius: 10px; background: #f8faff; }.coupon-item span { color: var(--qh-text-secondary); font-size: 13px; text-align: right; }.blog-item { display: grid; grid-template-columns: 74px 1fr; gap: 12px; align-items: center; }.blog-item .el-image { width: 74px; height: 54px; border-radius: 8px; }.blog-item p { display: -webkit-box; margin: 6px 0 0; overflow: hidden; color: var(--qh-text-secondary); font-size: 13px; line-height: 1.5; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
@media (max-width: 780px) { .campus-hero { grid-template-columns: 1fr; padding: 26px; }.home-bottom-grid { grid-template-columns: 1fr; } }
</style>
