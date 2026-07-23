<script setup>
import { reactive, ref, watch } from 'vue'
import { Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminShops } from '../api/admin-shop'
import { createAdminGoodsCategory, getAdminGoodsCategories, updateAdminGoodsCategory, updateAdminGoodsCategoryStatus } from '../api/admin-goods'

const stores = ref([]); const items = ref([]); const loading = ref(false); const saving = ref(false); const dialogVisible = ref(false); const formRef = ref()
const shopId = ref(null); const form = reactive({ id: null, name: '', sortOrder: 0 })
const rules = { name: [{ required: true, message: '请填写分类名称', trigger: 'blur' }], sortOrder: [{ required: true, message: '请填写排序值', trigger: 'change' }] }
async function loadStores() { const page = await getAdminShops({ page: 1, size: 100, status: 1 }); stores.value = page.records || [] }
async function load() { if (!shopId.value) { items.value = []; return } loading.value = true; try { items.value = await getAdminGoodsCategories({ shopId: shopId.value }) } catch (error) { ElMessage.error(error.message || '分类加载失败') } finally { loading.value = false } }
function openCreate() { Object.assign(form, { id: null, name: '', sortOrder: 0 }); dialogVisible.value = true }
function openEdit(item) { Object.assign(form, { id: item.id, name: item.name, sortOrder: item.sortOrder }); dialogVisible.value = true }
async function save() { const valid = await formRef.value.validate().catch(() => false); if (!valid || saving.value || !shopId.value) return; saving.value = true; try { if (form.id) await updateAdminGoodsCategory(form.id, { name: form.name, sortOrder: form.sortOrder }); else await createAdminGoodsCategory({ shopId: shopId.value, name: form.name, sortOrder: form.sortOrder }); ElMessage.success('商品分类已保存'); dialogVisible.value = false; await load() } catch (error) { ElMessage.error(error.message || '商品分类保存失败') } finally { saving.value = false } }
async function switchStatus(item) { const status = item.status === 1 ? '0' : '1'; try { await ElMessageBox.confirm(`确认${status === '1' ? '启用' : '停用'}“${item.name}”吗？停用后历史商品会保留原分类。`, '调整分类状态', { type: 'warning' }); await updateAdminGoodsCategoryStatus(item.id, { status }); await load() } catch (error) { if (error !== 'cancel') ElMessage.error(error.message || '分类状态调整失败') } }
watch(shopId, load)
loadStores().catch((error) => ElMessage.error(error.message || '店铺加载失败'))
</script>

<template>
  <section class="category-page"><div class="page-heading"><div><p>GOODS CATEGORY</p><h1>店内商品分类</h1><span>分类只属于一个店铺；同店名称不能重复，停用不会删除历史商品。</span></div><el-button type="primary" :icon="Plus" :disabled="!shopId" @click="openCreate">新增分类</el-button></div><el-card shadow="never"><div class="toolbar"><el-select v-model="shopId" filterable clearable placeholder="请选择店铺查看分类"><el-option v-for="store in stores" :key="store.id" :label="store.name" :value="store.id" /></el-select><el-button :icon="Refresh" :disabled="!shopId" @click="load">刷新</el-button></div><el-empty v-if="!shopId" description="请先选择一个店铺" /><el-table v-else v-loading="loading" :data="items"><el-table-column prop="name" label="分类名称" min-width="180" /><el-table-column prop="sortOrder" label="排序值" width="100" /><el-table-column prop="goodsCount" label="商品数量" width="100" /><el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="190"><template #default="{ row }"><el-button text type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button><el-button text @click="switchStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button></template></el-table-column></el-table></el-card><el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品分类' : '新增商品分类'" width="420px"><el-form ref="formRef" :model="form" :rules="rules" label-position="top"><el-form-item label="分类名称" prop="name"><el-input v-model="form.name" maxlength="64" show-word-limit /></el-form-item><el-form-item label="排序值" prop="sortOrder"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item></el-form><template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template></el-dialog></section>
</template>

<style scoped>
.category-page{max-width:1100px;margin:0 auto}.page-heading{display:flex;justify-content:space-between;gap:16px;margin-bottom:20px}.page-heading p{margin:0;color:#5a7db7;font-size:12px;font-weight:700;letter-spacing:.12em}.page-heading h1{margin:4px 0;color:#213758}.page-heading span{color:#7e8ea5}.toolbar{display:flex;gap:12px;margin-bottom:18px}.toolbar .el-select{width:min(360px,100%)}@media(max-width:640px){.page-heading{flex-direction:column}.toolbar{align-items:stretch;flex-direction:column}}
</style>
