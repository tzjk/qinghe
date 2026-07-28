<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Edit, Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategories } from '../api/category'
import { createAdminShop, getAdminShops, updateAdminShop, updateAdminShopStatus, uploadAdminShopCover } from '../api/admin-shop'

const formRef = ref()
const categories = ref([])
const result = ref({ records: [], total: 0 })
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const coverPreparing = ref(false)
const coverFile = ref(null)
const previewUrl = ref('')
const operating = reactive({})
const query = reactive({ page: 1, size: 10, keyword: '', categoryId: null, status: null })
const form = reactive(defaultForm())
const rules = {
  name: [{ required: true, message: '请填写店铺名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择店铺分类', trigger: 'change' }],
  address: [{ required: true, message: '请填写校园营业位置', trigger: 'blur' }],
  score: [{ required: true, message: '请填写店铺评分', trigger: 'change' }],
  status: [{ required: true, message: '请选择店铺状态', trigger: 'change' }],
  isFeatured: [{ required: true, message: '请选择推荐状态', trigger: 'change' }],
  sortOrder: [{ required: true, message: '请填写排序值', trigger: 'change' }]
}
const coverPreview = computed(() => previewUrl.value || form.coverImage || '')

function defaultForm() {
  return { id: null, name: '', categoryId: null, address: '', phone: '', score: 5, status: 1, isFeatured: 0, sortOrder: 0, longitude: null, latitude: null, coverImage: '' }
}

async function load() {
  loading.value = true
  try {
    result.value = await getAdminShops({ ...query })
  } catch (error) {
    result.value = { records: [], total: 0 }
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try { categories.value = await getCategories() } catch { categories.value = [] }
}

function search() { query.page = 1; load() }
function resetSearch() { Object.assign(query, { page: 1, size: query.size, keyword: '', categoryId: null, status: null }); load() }
function changeSize(size) { query.size = size; query.page = 1; load() }

function releasePreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}

function resetCover() {
  coverFile.value = null
  releasePreview()
}

function openCreate() {
  Object.assign(form, defaultForm())
  resetCover()
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, defaultForm(), row)
  resetCover()
  dialogVisible.value = true
}

async function onCoverChange(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  const allowed = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowed.includes(file.type)) { ElMessage.error('仅支持 jpg、jpeg、png 或 webp 图片'); return }
  if (file.size <= 0 || file.size > 3 * 1024 * 1024) { ElMessage.error('店铺封面原文件不能超过 3MB'); return }
  coverPreparing.value = true
  try {
    const optimized = await cropAndCompress(file)
    resetCover()
    coverFile.value = optimized
    previewUrl.value = URL.createObjectURL(optimized)
  } catch (error) {
    ElMessage.error(error.message || '图片预处理失败，请更换图片')
  } finally {
    coverPreparing.value = false
  }
}

async function cropAndCompress(file) {
  const sourceUrl = URL.createObjectURL(file)
  try {
    const image = await new Promise((resolve, reject) => {
      const element = new Image()
      element.onload = () => resolve(element)
      element.onerror = () => reject(new Error('图片内容无效，请重新选择'))
      element.src = sourceUrl
    })
    const targetWidth = 1200
    const targetHeight = 800
    const targetRatio = targetWidth / targetHeight
    const sourceRatio = image.naturalWidth / image.naturalHeight
    let sx = 0; let sy = 0; let sw = image.naturalWidth; let sh = image.naturalHeight
    if (sourceRatio > targetRatio) { sw = image.naturalHeight * targetRatio; sx = (image.naturalWidth - sw) / 2 }
    else { sh = image.naturalWidth / targetRatio; sy = (image.naturalHeight - sh) / 2 }
    const canvas = document.createElement('canvas')
    canvas.width = targetWidth; canvas.height = targetHeight
    canvas.getContext('2d').drawImage(image, sx, sy, sw, sh, 0, 0, targetWidth, targetHeight)
    let quality = 0.86
    let blob = await toWebp(canvas, quality)
    while (blob.size > 500 * 1024 && quality > 0.5) { quality -= 0.08; blob = await toWebp(canvas, quality) }
    if (blob.size > 500 * 1024) throw new Error('图片压缩后仍超过 500KB，请选择更简单的图片')
    return new File([blob], 'shop-cover.webp', { type: 'image/webp' })
  } finally {
    URL.revokeObjectURL(sourceUrl)
  }
}

function toWebp(canvas, quality) {
  return new Promise((resolve, reject) => canvas.toBlob((blob) => {
    if (!blob || blob.type !== 'image/webp') reject(new Error('当前浏览器不支持 WebP 压缩，请更换浏览器'))
    else resolve(blob)
  }, 'image/webp', quality))
}

async function save() {
  if (!formRef.value || saving.value || coverPreparing.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { name: form.name, categoryId: form.categoryId, address: form.address, phone: form.phone || null, score: form.score, status: form.status, isFeatured: form.isFeatured, sortOrder: form.sortOrder, longitude: form.longitude, latitude: form.latitude }
    const saved = form.id ? await updateAdminShop(form.id, payload) : await createAdminShop(payload)
    Object.assign(form, saved)
    if (coverFile.value) {
      try {
        const withCover = await uploadAdminShopCover(saved.id, coverFile.value)
        Object.assign(form, withCover)
      } catch (error) {
        await load()
        ElMessage.warning(error.message || '店铺资料已保存，封面上传失败；已保留当前表单内容，可重试上传')
        return
      }
    }
    ElMessage.success(form.id ? '店铺资料已保存' : '店铺已新增')
    dialogVisible.value = false
    resetCover()
    await load()
  } catch (error) {
    ElMessage.error(error.message || '店铺资料保存失败')
  } finally {
    saving.value = false
  }
}

async function switchStatus(row) {
  const next = Number(row.status) === 1 ? 0 : 1
  await ElMessageBox.confirm(next === 1 ? `确认启用“${row.name}”吗？` : `停用后用户端将不再展示“${row.name}”，确认继续吗？`, '调整店铺状态', { type: 'warning' })
  operating[row.id] = true
  try {
    await updateAdminShopStatus(row.id, { status: next })
    ElMessage.success(next === 1 ? '店铺已启用' : '店铺已停用')
    await load()
  } catch (error) {
    ElMessage.error(error.message || '店铺状态调整失败')
  } finally {
    operating[row.id] = false
  }
}

onMounted(async () => { await loadCategories(); await load() })
</script>

<template>
  <section class="admin-shop-page">
    <div class="page-heading"><div><p>SHOP ADMIN</p><h1>店铺管理</h1><span>维护店铺真实资料、展示状态与单张封面。</span></div><el-button type="primary" :icon="Plus" @click="openCreate">新增店铺</el-button></div>
    <el-card class="filter-card" shadow="never"><el-form class="filter-form" @submit.prevent="search"><el-form-item label="名称"><el-input v-model="query.keyword" clearable :prefix-icon="Search" placeholder="店铺名称" @keyup.enter="search" /></el-form-item><el-form-item label="分类"><el-select v-model="query.categoryId" clearable placeholder="全部分类"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item><el-form-item label="状态"><el-select v-model="query.status" clearable placeholder="全部状态"><el-option label="营业中" :value="1" /><el-option label="已停用" :value="0" /></el-select></el-form-item><el-button type="primary" :icon="Search" :loading="loading" @click="search">查询</el-button><el-button @click="resetSearch">重置</el-button><el-button text :icon="Refresh" :loading="loading" @click="load">刷新</el-button></el-form></el-card>
    <el-card class="table-card" shadow="never"><el-table v-loading="loading" :data="result.records" empty-text="暂无店铺数据"><el-table-column label="封面" width="116"><template #default="{ row }"><el-image v-if="row.coverImage" class="cover-thumb" :src="row.coverImage" fit="cover" preview-teleported><template #error><div class="cover-placeholder">店铺封面</div></template></el-image><div v-else class="cover-thumb cover-placeholder">店铺封面</div></template></el-table-column><el-table-column prop="name" label="店铺名称" min-width="150" /><el-table-column prop="categoryName" label="分类" min-width="110" /><el-table-column prop="address" label="校园营业位置" min-width="180" show-overflow-tooltip /><el-table-column label="评分" width="88"><template #default="{ row }">{{ Number(row.score || 0).toFixed(1) }}</template></el-table-column><el-table-column label="状态" width="96"><template #default="{ row }"><el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">{{ Number(row.status) === 1 ? '营业中' : '已停用' }}</el-tag></template></el-table-column><el-table-column label="推荐" width="80"><template #default="{ row }">{{ Number(row.isFeatured) === 1 ? '是' : '否' }}</template></el-table-column><el-table-column label="操作" fixed="right" width="196"><template #default="{ row }"><el-button text type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button><el-button text :loading="operating[row.id]" :type="Number(row.status) === 1 ? 'danger' : 'success'" @click="switchStatus(row)">{{ Number(row.status) === 1 ? '停用' : '启用' }}</el-button></template></el-table-column></el-table><el-pagination v-if="result.total" v-model:current-page="query.page" :page-size="query.size" :page-sizes="[10, 20, 50]" :total="result.total" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="changeSize" /></el-card>
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑店铺' : '新增店铺'" width="min(760px, calc(100vw - 28px))" destroy-on-close @closed="resetCover"><el-form ref="formRef" :model="form" :rules="rules" label-position="top"><div class="dialog-grid"><el-form-item label="店铺名称" prop="name"><el-input v-model="form.name" maxlength="100" show-word-limit /></el-form-item><el-form-item label="店铺分类" prop="categoryId"><el-select v-model="form.categoryId" placeholder="请选择已启用分类"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item><el-form-item class="wide" label="校园营业位置" prop="address"><el-input v-model="form.address" maxlength="255" show-word-limit placeholder="例如：主校区商业街 A 座" /></el-form-item><el-form-item label="联系电话" prop="phone"><el-input v-model="form.phone" maxlength="20" placeholder="可留空" /></el-form-item><el-form-item label="店铺评分" prop="score"><el-input-number v-model="form.score" :min="0" :max="5" :precision="2" :step="0.1" /></el-form-item><el-form-item label="展示状态" prop="status"><el-radio-group v-model="form.status"><el-radio :label="1">营业中</el-radio><el-radio :label="0">已停用</el-radio></el-radio-group></el-form-item><el-form-item label="首页推荐" prop="isFeatured"><el-radio-group v-model="form.isFeatured"><el-radio :label="1">推荐</el-radio><el-radio :label="0">不推荐</el-radio></el-radio-group></el-form-item><el-form-item label="排序值" prop="sortOrder"><el-input-number v-model="form.sortOrder" :min="0" :max="999999" /></el-form-item></div><el-form-item label="店铺封面"><div class="cover-editor"><div v-if="coverPreview" class="cover-preview"><el-image :src="coverPreview" fit="cover" /><span>本地预览，仅保存时上传</span></div><div v-else class="cover-preview cover-placeholder">店铺封面</div><div><el-upload accept=".jpg,.jpeg,.png,.webp" :auto-upload="false" :show-file-list="false" :disabled="coverPreparing || saving" :on-change="onCoverChange"><el-button :icon="Upload" :loading="coverPreparing">选择并裁剪封面</el-button></el-upload><el-button v-if="coverFile" text type="danger" @click="resetCover">移除本地图片</el-button><p>支持 jpg/jpeg/png/webp；本地居中裁剪为约 3:2、1200×800 并优先压缩为 WebP（不超过 500KB）。</p></div></div></el-form-item></el-form><template #footer><el-button :disabled="saving" @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" :disabled="coverPreparing" @click="save">保存</el-button></template></el-dialog>
  </section>
</template>

<style scoped>
.admin-shop-page { max-width: 1440px; margin: 0 auto; }.page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 22px; }.page-heading p { margin: 0 0 6px; color: #5a7db7; font-size: 12px; font-weight: 700; letter-spacing: .12em; }.page-heading h1 { margin: 0; color: #213758; font-size: 28px; }.page-heading span { display: block; margin-top: 8px; color: #7e8ea5; }.filter-card, .table-card { border-color: #e7edf5; }.filter-card { margin-bottom: 16px; }.filter-form { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 0 12px; }.filter-form .el-form-item { margin-bottom: 0; }.filter-form .el-input, .filter-form .el-select { width: 190px; }.cover-thumb { width: 92px; height: 60px; border-radius: 8px; overflow: hidden; }.cover-placeholder { display: flex; align-items: center; justify-content: center; color: #7b8aa0; background: linear-gradient(135deg, #edf3fb, #dfe9f7); font-size: 12px; }.el-pagination { justify-content: flex-end; margin-top: 18px; }.dialog-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 18px; }.wide { grid-column: 1 / -1; }.cover-editor { display: flex; align-items: center; gap: 16px; }.cover-preview { width: 210px; height: 140px; overflow: hidden; border: 1px solid #dce6f2; border-radius: 10px; }.cover-preview .el-image { width: 100%; height: 112px; }.cover-preview span { display: block; padding: 5px 8px; color: #6f8098; font-size: 11px; }.cover-editor p { max-width: 340px; margin: 10px 0 0; color: #7e8ea5; font-size: 12px; line-height: 1.6; } @media (max-width: 720px) { .page-heading { align-items: stretch; flex-direction: column; }.page-heading .el-button { width: 100%; }.filter-form { display: grid; grid-template-columns: 1fr; }.filter-form .el-input, .filter-form .el-select { width: 100%; }.filter-form .el-button { margin-top: 8px; }.dialog-grid { grid-template-columns: 1fr; }.wide { grid-column: auto; }.cover-editor { align-items: stretch; flex-direction: column; }.cover-preview { width: 100%; height: 200px; }.cover-preview .el-image { height: 172px; } }
</style>
