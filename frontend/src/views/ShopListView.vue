<script setup>
import { onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getCategories } from '../api/category'
import { getShops } from '../api/shop'
import AsyncState from '../components/AsyncState.vue'
import PageHeader from '../components/PageHeader.vue'
import ShopCard from '../components/ShopCard.vue'

const categories = ref([])
const result = ref({ records: [], total: 0 })
const query = ref({ page: 1, size: 8, categoryId: null, keyword: '', sort: '' })
const loading = ref(false)
const error = ref('')
async function load() { loading.value = true; error.value = ''; try { result.value = await getShops({ ...query.value }) } catch (e) { error.value = e.message || '商铺列表加载失败' } finally { loading.value = false } }
async function init() { try { categories.value = await getCategories() } catch { categories.value = [] } finally { load() } }
function applyFilters() { query.value.page = 1; load() }
function changeSize(size) { query.value.size = size; query.value.page = 1; load() }
onMounted(init)
</script>

<template>
  <section class="page-shell">
    <PageHeader title="商铺" description="按分类、关键词和排序寻找校园周边服务。" eyebrow="DISCOVER SHOPS"><template #actions><el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button></template></PageHeader>
    <el-card class="ui-card filter-card"><el-form class="shop-filter" @submit.prevent="applyFilters"><el-form-item label="分类"><el-select v-model="query.categoryId" clearable placeholder="全部分类" @change="applyFilters"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item><el-form-item label="关键词"><el-input v-model="query.keyword" clearable placeholder="输入商铺名称" :prefix-icon="Search" @keyup.enter="applyFilters" /></el-form-item><el-form-item label="排序"><el-select v-model="query.sort" @change="applyFilters"><el-option label="默认排序" value="" /><el-option label="评分优先" value="score" /><el-option label="最新发布" value="latest" /></el-select></el-form-item><el-button type="primary" :loading="loading" @click="applyFilters">查询</el-button></el-form></el-card>
    <div class="list-overview"><span>共 <strong>{{ result.total || 0 }}</strong> 家商铺</span><span class="meta">当前结果由服务端分页返回</span></div>
    <AsyncState :loading="loading" :error="error" :empty="!result.records?.length" empty-text="暂无匹配商铺" :skeleton-rows="8" @retry="load"><template #empty-action><el-button type="primary" @click="query.keyword='';query.categoryId=null;applyFilters()">清除筛选</el-button></template><div class="content-grid shop-grid"><ShopCard v-for="item in result.records" :key="item.id" :shop="item" /></div><el-pagination v-if="result.total > 0" v-model:current-page="query.page" :page-size="query.size" :page-sizes="[8, 12, 16, 24]" :total="result.total" layout="total, sizes, prev, pager, next, jumper" @current-change="load" @size-change="changeSize" /></AsyncState>
  </section>
</template>

<style scoped>
.filter-card { margin-bottom: 18px; }.shop-filter { display: flex; align-items: flex-end; gap: 4px 14px; }.shop-filter .el-form-item { margin-bottom: 0; }.shop-filter .el-select { width: 145px; }.shop-filter .el-input { width: 220px; }.list-overview { display: flex; justify-content: space-between; gap: 12px; margin: 22px 0 16px; }.list-overview strong { color: var(--qh-primary); font-size: 18px; } @media (max-width: 680px) { .shop-filter { display: grid; grid-template-columns: 1fr; }.shop-filter .el-select, .shop-filter .el-input { width: 100%; }.list-overview { align-items: flex-start; flex-direction: column; } }
</style>
