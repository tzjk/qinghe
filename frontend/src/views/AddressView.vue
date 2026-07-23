<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { Location, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createAddress, deleteAddress, getAddresses, getCampusBuildings, getCampuses, setDefaultAddress, updateAddress } from '../api/address'
import AsyncState from '../components/AsyncState.vue'
import PageHeader from '../components/PageHeader.vue'

const addresses = ref([])
const campuses = ref([])
const buildings = ref([])
const loading = ref(false)
const saving = ref(false)
const campusLoading = ref(false)
const buildingLoading = ref(false)
const error = ref('')
const campusError = ref('')
const buildingError = ref('')
const dialogVisible = ref(false)
const editingId = ref(null)
const editingHistorical = ref(false)
const formRef = ref()

const emptyForm = () => ({
  receiverName: '', receiverPhone: '', campusId: null, buildingId: null, floor: '', roomNo: '', deliveryPoint: '',
  detail: '', label: '', remark: '', isDefault: false
})
const form = reactive(emptyForm())
const locationValidator = (rule, value, callback) => {
  if (!String(form.roomNo || '').trim() && !String(form.deliveryPoint || '').trim()) callback(new Error('房间号或配送点至少填写一项'))
  else callback()
}
const rules = {
  receiverName: [{ required: true, message: '请输入收件人', trigger: 'blur' }],
  receiverPhone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确手机号', trigger: 'blur' }],
  campusId: [{ required: true, message: '请选择校区', trigger: 'change' }],
  buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
  roomNo: [{ validator: locationValidator, trigger: 'blur' }],
  deliveryPoint: [{ validator: locationValidator, trigger: 'blur' }]
}

async function load() {
  loading.value = true; error.value = ''
  try { addresses.value = await getAddresses() } catch (e) { error.value = e.message || '地址加载失败' } finally { loading.value = false }
}
async function loadCampuses() {
  campusLoading.value = true; campusError.value = ''
  try { campuses.value = await getCampuses() } catch (e) { campusError.value = e.message || '校区加载失败' } finally { campusLoading.value = false }
}
async function loadBuildings(campusId) {
  buildings.value = []; buildingError.value = ''
  if (!campusId) return
  buildingLoading.value = true
  try { buildings.value = await getCampusBuildings(campusId) } catch (e) { buildingError.value = e.message || '楼栋加载失败' } finally { buildingLoading.value = false }
}
function resetForm() {
  Object.assign(form, emptyForm()); editingId.value = null; editingHistorical.value = false; buildings.value = []; buildingError.value = ''
}
async function openCreate() {
  resetForm(); dialogVisible.value = true
  if (!campuses.value.length && !campusLoading.value) await loadCampuses()
}
async function openEdit(item) {
  editingId.value = item.id; editingHistorical.value = item.addressType === 'HISTORICAL'
  Object.assign(form, {
    receiverName: item.receiverName || '', receiverPhone: item.receiverPhone || '', campusId: item.campusId || null,
    buildingId: item.buildingId || null, floor: item.floor || '', roomNo: item.roomNo || '',
    deliveryPoint: item.deliveryPoint || '', detail: item.detail || '', label: item.label || '', remark: item.remark || '',
    isDefault: Boolean(item.isDefault)
  })
  dialogVisible.value = true
  if (!campuses.value.length && !campusLoading.value) await loadCampuses()
  if (form.campusId) {
    await loadBuildings(form.campusId)
    form.buildingId = item.buildingId || null
  }
}
watch(() => form.campusId, async (campusId, previousCampusId) => {
  if (campusId !== previousCampusId) form.buildingId = null
  await loadBuildings(campusId)
})
async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  const payload = {
    receiverName: form.receiverName, receiverPhone: form.receiverPhone, campusId: form.campusId, buildingId: form.buildingId,
    floor: form.floor, roomNo: form.roomNo, deliveryPoint: form.deliveryPoint, detail: form.detail, label: form.label,
    remark: form.remark, isDefault: form.isDefault
  }
  try {
    if (editingId.value) await updateAddress(editingId.value, payload)
    else await createAddress(payload)
    ElMessage.success(editingId.value ? '校园地址已更新' : '校园地址已添加')
    dialogVisible.value = false
    await load()
  } catch (e) { error.value = e.message || '保存失败' } finally { saving.value = false }
}
async function setDefault(item) {
  try { await setDefaultAddress(item.id); ElMessage.success('默认地址已更新'); await load() } catch (e) { error.value = e.message || '设置失败' }
}
async function remove(item) {
  try {
    await ElMessageBox.confirm(`确认删除“${item.receiverName}”的地址吗？`, '删除地址', { type: 'warning' })
    await deleteAddress(item.id); ElMessage.success('地址已删除'); await load()
  } catch (e) { if (e !== 'cancel' && e !== 'close') error.value = e.message || '删除失败' }
}
function displayPhone(item) { return item.maskedReceiverPhone || String(item.receiverPhone || '').replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2') }
function buildingText(item) { return [item.area, item.buildingType, item.buildingName].filter(Boolean).join(' · ') }
onMounted(async () => { await Promise.all([load(), loadCampuses()]) })
</script>

<template>
  <section class="page-shell">
    <PageHeader title="我的地址" description="校园配送地址仅在当前账号内管理，列表号码已脱敏展示。" eyebrow="MY ADDRESSES">
      <template #actions><el-button type="primary" :icon="Plus" @click="openCreate">新增地址</el-button></template>
    </PageHeader>
    <AsyncState :loading="loading" :error="error" :empty="!addresses.length" empty-text="暂无地址" :skeleton-rows="7" @retry="load">
      <template #empty-action><el-button type="primary" @click="openCreate">添加地址</el-button></template>
      <div class="address-list">
        <el-card v-for="item in addresses" :key="item.id" class="ui-card address-card" :class="{ 'is-default': item.isDefault, historical: item.addressType === 'HISTORICAL' }">
          <div class="address-top"><div><strong>{{ item.receiverName }}</strong><span>{{ displayPhone(item) }}</span></div><div class="tag-row"><el-tag v-if="item.addressType === 'HISTORICAL'" type="warning" effect="light">历史地址，请更新</el-tag><el-tag v-if="item.isDefault" type="success" effect="light" class="status-tag">默认地址</el-tag></div></div>
          <p class="address-detail"><el-icon><Location /></el-icon>{{ item.formattedAddress || '地址信息待完善' }}</p>
          <div v-if="item.label || item.remark" class="address-meta"><el-tag v-if="item.label" effect="plain">{{ item.label }}</el-tag><span v-if="item.remark">备注：{{ item.remark }}</span></div>
          <div class="address-actions"><el-button text type="primary" :disabled="Boolean(item.isDefault)" @click="setDefault(item)">设为默认</el-button><el-button text @click="openEdit(item)">{{ item.addressType === 'HISTORICAL' ? '更新地址' : '编辑' }}</el-button><el-button text type="danger" @click="remove(item)">删除</el-button></div>
        </el-card>
      </div>
    </AsyncState>
    <el-dialog v-model="dialogVisible" :title="editingId ? (editingHistorical ? '更新历史地址' : '编辑校园地址') : '新增校园地址'" width="min(600px, 94vw)" :close-on-click-modal="false">
      <el-alert v-if="editingHistorical" title="历史地址，请更新为校园地址后保存；原省市区历史数据会保留。" type="warning" :closable="false" show-icon class="history-alert" />
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="收件人" prop="receiverName"><el-input v-model="form.receiverName" maxlength="64" /></el-form-item>
        <el-form-item label="手机号" prop="receiverPhone"><el-input v-model="form.receiverPhone" maxlength="11" /></el-form-item>
        <el-form-item label="校区" prop="campusId"><el-select v-model="form.campusId" class="full-width" placeholder="请选择校区" :loading="campusLoading" :disabled="campusLoading"><el-option v-for="item in campuses" :key="item.id" :label="item.campusName" :value="item.id" /><template #empty><el-empty v-if="!campusLoading && !campusError" description="暂无可用校区" :image-size="56" /></template></el-select><div v-if="campusError" class="field-error">{{ campusError }} <el-button link type="primary" @click="loadCampuses">重试</el-button></div></el-form-item>
        <el-form-item label="楼栋" prop="buildingId"><el-select v-model="form.buildingId" class="full-width" placeholder="请先选择校区" :loading="buildingLoading" :disabled="!form.campusId || buildingLoading"><el-option v-for="item in buildings" :key="item.id" :label="buildingText(item)" :value="item.id" /><template #empty><el-empty v-if="form.campusId && !buildingLoading && !buildingError" description="该校区暂无可用楼栋" :image-size="56" /></template></el-select><div v-if="buildingError" class="field-error">{{ buildingError }} <el-button link type="primary" @click="loadBuildings(form.campusId)">重试</el-button></div></el-form-item>
        <div class="form-grid"><el-form-item label="楼层"><el-input v-model="form.floor" maxlength="20" placeholder="如：3层" /></el-form-item><el-form-item label="房间号或宿舍号" prop="roomNo"><el-input v-model="form.roomNo" maxlength="64" /></el-form-item></div>
        <el-form-item label="配送点或自提点" prop="deliveryPoint"><el-input v-model="form.deliveryPoint" maxlength="128" placeholder="房间号和配送点至少填写一项" /></el-form-item>
        <el-form-item label="详细位置说明"><el-input v-model="form.detail" maxlength="500" type="textarea" :rows="2" /></el-form-item>
        <div class="form-grid"><el-form-item label="地址标签"><el-input v-model="form.label" maxlength="32" placeholder="如：宿舍、实验室" /></el-form-item><el-form-item label="配送备注"><el-input v-model="form.remark" maxlength="255" /></el-form-item></div>
        <el-form-item><el-checkbox v-model="form.isDefault">设为默认地址</el-checkbox></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.address-list { display: grid; gap: 14px; }.address-card { border-left: 4px solid #c8d7ee !important; }.address-card.is-default { border-left-color: var(--qh-success) !important; }.address-card.historical { border-left-color: var(--qh-warning) !important; }.address-top, .address-top > div, .tag-row { display: flex; align-items: center; gap: 12px; }.address-top { justify-content: space-between; }.address-top span, .address-meta { color: var(--qh-text-secondary); }.address-detail { display: flex; align-items: flex-start; gap: 7px; margin: 15px 0 10px; color: var(--qh-text-secondary); line-height: 1.65; }.address-detail .el-icon { margin-top: 4px; color: var(--qh-primary); }.address-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; font-size: 13px; }.address-actions { display: flex; justify-content: flex-end; border-top: 1px solid var(--qh-border); padding-top: 10px; margin-top: 12px; }.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }.full-width { width: 100%; }.field-error { color: var(--qh-danger); font-size: 12px; line-height: 1.6; margin-top: 4px; }.history-alert { margin-bottom: 16px; } @media (max-width: 600px) { .form-grid { grid-template-columns: 1fr; gap: 0; }.address-top, .address-top > div { align-items: flex-start; flex-direction: column; gap: 5px; }.tag-row { flex-direction: row !important; flex-wrap: wrap; }.address-actions { justify-content: flex-start; } }
</style>
