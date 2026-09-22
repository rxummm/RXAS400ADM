<template>
  <div class="page-container page-container--fit">
    <!-- 搜索区 -->
    <div class="search-bar">
      <el-input v-model="query.poNo" class="w-150" :placeholder="$t('procurement.po.poNo')" clearable @keyup.enter="search" />
      <el-input v-model="query.vendorName" class="w-150" :placeholder="$t('procurement.po.vendorName')" clearable @keyup.enter="search" />
      <el-select v-model="query.status" :placeholder="$t('procurement.po.status')" clearable class="w-130">
        <el-option v-for="s in statusOptions" :key="s.value" :label="$t(s.label)" :value="s.value" />
      </el-select>
      <el-date-picker v-model="query.orderDateFrom" type="date" :placeholder="$t('common.startDate')" class="w-140" />
      <el-date-picker v-model="query.orderDateTo" type="date" :placeholder="$t('common.endDate')" class="w-140" />
      <el-button type="primary" :loading="loading" @click="search">{{ $t('common.search') }}</el-button>
      <el-button @click="reset">{{ $t('common.reset') }}</el-button>
      <div class="flex-1" />
      <el-button type="primary" @click="openCreate" v-if="hasPerm('PO_MANAGE')">
        <el-icon><Plus /></el-icon>{{ $t('procurement.po.create') }}
      </el-button>
    </div>

    <!-- 表格 -->
    <div class="table-wrapper">
      <el-table :data="orders" v-loading="loading" size="small" border highlight-current-row @row-click="openDetail">
        <el-table-column prop="poNo" :label="$t('procurement.po.poNo')" min-width="160" />
        <el-table-column prop="vendorName" :label="$t('procurement.po.vendorName')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="orderDate" :label="$t('procurement.po.orderDate')" width="110" />
        <el-table-column prop="reqDate" :label="$t('procurement.po.reqDate')" width="110" />
        <el-table-column align="right" :label="$t('procurement.po.totalAmount')" width="130">
          <template #default="{ row }">{{ fmtMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column :label="$t('procurement.po.status')" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ $t(row.statusKey) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" :label="$t('procurement.po.createdBy')" width="100" />
        <el-table-column prop="createdTime" :label="$t('procurement.po.createdTime')" width="170">
          <template #default="{ row }">{{ row.createdTime?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row as PurchaseOrderVO)">{{ $t('common.detail') }}</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="primary" size="small" @click.stop="openEdit(row as PurchaseOrderVO)">{{ $t('procurement.po.edit') }}</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="danger" size="small" @click.stop="handleDelete(row as PurchaseOrderVO)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-if="total > 0" :total="total" v-model:current="current" v-model:size="size" @change="search" @size-change="search" />
      <el-empty v-if="searched && !loading && orders.length === 0" :description="$t('common.noData')" />
    </div>

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="formVisible" :title="isEdit ? $t('procurement.po.edit') : $t('procurement.po.create')" width="800px" :close-on-click-modal="false" top="5vh">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px" size="small" v-loading="saving">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('procurement.po.poNo')" prop="poNo">
              <el-input v-model="form.poNo" :disabled="isEdit">
                <template #append>
                  <el-button v-if="!isEdit" @click="autoGeneratePoNo">{{ $t('common.refresh') }}</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('procurement.po.currency')" prop="currency">
              <el-select v-model="form.currency" class="w-full">
                <el-option label="CNY" value="CNY" />
                <el-option label="USD" value="USD" />
                <el-option label="EUR" value="EUR" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('procurement.po.vendorCode')" prop="vendorCode">
              <el-input v-model="form.vendorCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('procurement.po.vendorName')" prop="vendorName">
              <el-input v-model="form.vendorName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('procurement.po.orderDate')" prop="orderDate">
              <el-date-picker v-model="form.orderDate" type="date" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('procurement.po.reqDate')" prop="reqDate">
              <el-date-picker v-model="form.reqDate" type="date" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 行项表格 -->
        <el-divider>{{ $t('procurement.po.lineItems') }}</el-divider>
        <el-table :data="form.items" size="small" border class="w-full">
          <el-table-column :label="$t('procurement.po.itemCode')" width="120">
            <template #default="{ row }">
              <el-input v-model="row.itemCode" size="small" @input="validateItems" />
            </template>
          </el-table-column>
          <el-table-column :label="$t('procurement.po.itemDesc')" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.itemDesc" size="small" />
            </template>
          </el-table-column>
          <el-table-column :label="$t('procurement.po.qtyOrdered')" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.qtyOrdered" :min="0" :precision="2" size="small" class="w-80" @change="recalcLine" />
            </template>
          </el-table-column>
          <el-table-column :label="$t('procurement.po.unitPrice')" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.unitPrice" :min="0" :precision="4" size="small" class="w-90" @change="recalcLine" />
            </template>
          </el-table-column>
          <el-table-column :label="$t('procurement.po.uom')" width="70">
            <template #default="{ row }">
              <el-input v-model="row.uom" size="small" />
            </template>
          </el-table-column>
          <el-table-column :label="$t('common.operation')" width="50">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="form.items.splice($index, 1)">{{ $t('common.delete') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button class="mt8" size="small" @click="addItem">{{ $t('common.add') }}</el-button>

        <el-form-item :label="$t('procurement.po.notes')" prop="notes" class="mt16">
          <el-input v-model="form.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="$t('procurement.po.detail') + ' · ' + (detail?.poNo || '')" size="700px" :close-on-click-modal="false">
      <template v-if="detail">
        <el-tabs v-model="detailTab">
          <el-tab-pane :label="$t('procurement.po.basicInfo')" name="info">
            <el-descriptions :column="2" size="small" border>
              <el-descriptions-item :label="$t('procurement.po.poNo')">{{ detail.poNo }}</el-descriptions-item>
              <el-descriptions-item :label="$t('procurement.po.status')">
                <el-tag size="small" :type="statusTag(detail.status)">{{ $t(detail.statusKey) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('procurement.po.vendorCode')">{{ detail.vendorCode || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="$t('procurement.po.vendorName')">{{ detail.vendorName || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="$t('procurement.po.orderDate')">{{ detail.orderDate || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="$t('procurement.po.reqDate')">{{ detail.reqDate || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="$t('procurement.po.totalAmount')">{{ fmtMoney(detail.totalAmount) }}</el-descriptions-item>
              <el-descriptions-item :label="$t('procurement.po.currency')">{{ detail.currency }}</el-descriptions-item>
              <el-descriptions-item :label="$t('procurement.po.approvedBy')">{{ detail.approvedBy || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="$t('procurement.po.approvedTime')">{{ detail.approvedTime?.replace('T', ' ') || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="$t('procurement.po.createdBy')" :span="1">{{ detail.createdBy }}</el-descriptions-item>
              <el-descriptions-item :label="$t('procurement.po.createdTime')" :span="1">{{ detail.createdTime?.replace('T', ' ') }}</el-descriptions-item>
            </el-descriptions>
            <div v-if="detail.notes" class="mt8">
              <strong>{{ $t('procurement.po.notes') }}：</strong>{{ detail.notes }}
            </div>
            <!-- 操作按钮 -->
            <div class="mt16">
              <el-button v-if="detail.status === 'DRAFT'" type="primary" size="small" @click="handleSubmitApproval(detail.id)">
                {{ $t('procurement.po.submitApproval') }}
              </el-button>
              <el-button v-if="detail.status === 'APPROVED' || detail.status === 'SHIPPED'" type="success" size="small" @click="handleReceive(detail.id)">
                {{ $t('procurement.po.receive') }}
              </el-button>
              <el-button v-if="detail.status === 'DRAFT' || detail.status === 'PENDING_APPROVAL'" type="danger" size="small" @click="handleCancel(detail.id)">
                {{ $t('procurement.po.cancel') }}
              </el-button>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="$t('procurement.po.lineItems')" name="items">
            <el-table :data="detail.items" size="small" border>
              <el-table-column prop="lineNo" :label="$t('procurement.po.lineNo')" width="60" />
              <el-table-column prop="itemCode" :label="$t('procurement.po.itemCode')" width="110" />
              <el-table-column prop="itemDesc" :label="$t('procurement.po.itemDesc')" min-width="130" show-overflow-tooltip />
              <el-table-column align="right" :label="$t('procurement.po.qtyOrdered')" width="90">
                <template #default="{ row }">{{ row.qtyOrdered ?? '—' }}</template>
              </el-table-column>
              <el-table-column align="right" :label="$t('procurement.po.qtyReceived')" width="90">
                <template #default="{ row }">{{ row.qtyReceived ?? '—' }}</template>
              </el-table-column>
              <el-table-column align="right" :label="$t('procurement.po.unitPrice')" width="100">
                <template #default="{ row }">{{ fmtMoney(row.unitPrice) }}</template>
              </el-table-column>
              <el-table-column align="right" :label="$t('procurement.po.lineAmount')" width="110">
                <template #default="{ row }">{{ fmtMoney(row.lineAmount) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="$t('procurement.po.approvalHistory')" name="approval">
            <el-timeline v-if="detail.approvals.length > 0">
              <el-timeline-item v-for="a in detail.approvals" :key="a.id" :timestamp="a.actionTime?.replace('T', ' ')" placement="top">
                <div>
                  <el-tag size="small" :type="approvalTag(a.action)" class="mr4">{{ a.action }}</el-tag>
                  <strong>{{ a.approver }}</strong>
                  <span class="text-muted ml4">({{ $t('procurement.approval.level' + a.level) }})</span>
                </div>
                <div v-if="a.comment" class="text-muted mt4">{{ a.comment }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else :description="$t('common.noData')" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ProcurementPo' })

import { reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  pagePurchaseOrders,
  getPurchaseOrderDetail,
  generatePoNo,
  createPurchaseOrder,
  updatePurchaseOrder,
  submitForApproval,
  receiveOrder,
  cancelOrder,
  deletePurchaseOrder,
  type PurchaseOrderVO,
  type PurchaseOrderDetail,
  type PurchaseOrderCreateDTO,
  type PurchaseOrderUpdateDTO,
  type PurchaseOrderQuery,
} from '@/api/procurement'
import { useUserStore } from '@/stores/user'
import AppPagination from '@/components/AppPagination.vue'
import { formatMoney } from '@/utils/format'
import type { FormInstance, FormRules } from 'element-plus'

const { t } = useI18n()
const userStore = useUserStore()
const hasPerm = (code: string) => userStore.hasPermission(code)

// 状态选项
const statusOptions = [
  { value: '', label: 'common.all' },
  { value: 'DRAFT', label: 'procurement.po.statusDraft' },
  { value: 'PENDING_APPROVAL', label: 'procurement.po.statusPendingApproval' },
  { value: 'APPROVED', label: 'procurement.po.statusApproved' },
  { value: 'SHIPPED', label: 'procurement.po.statusShipped' },
  { value: 'RECEIVED', label: 'procurement.po.statusReceived' },
  { value: 'PAID', label: 'procurement.po.statusPaid' },
  { value: 'CANCELLED', label: 'procurement.po.statusCancelled' },
]

// 表格数据
const loading = ref(false)
const searched = ref(false)
const orders = ref<PurchaseOrderVO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const query = reactive<PurchaseOrderQuery>({
  poNo: '', vendorName: '', status: '', orderDateFrom: '', orderDateTo: '',
})

function search() {
  loading.value = true
  searched.value = true
  pagePurchaseOrders({ current: current.value, size: size.value, ...query })
    .then(data => {
      orders.value = data.records
      total.value = data.total
    })
    .finally(() => { loading.value = false })
}

function reset() {
  query.poNo = ''
  query.vendorName = ''
  query.status = ''
  query.orderDateFrom = ''
  query.orderDateTo = ''
  current.value = 1
  search()
}

// 表单
const formVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<PurchaseOrderCreateDTO & { id?: number }>({
  poNo: '', cono: '001', vendorCode: '', vendorName: '',
  orderDate: '', reqDate: '', currency: 'CNY', notes: '', items: [],
})

const formRules: FormRules = {
  poNo: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
  vendorName: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
}

function resetForm() {
  form.id = undefined
  form.poNo = ''
  form.cono = '001'
  form.vendorCode = ''
  form.vendorName = ''
  form.orderDate = ''
  form.reqDate = ''
  form.currency = 'CNY'
  form.notes = ''
  form.items = []
}

async function autoGeneratePoNo() {
  try {
    form.poNo = await generatePoNo()
  } catch { /* request interceptor handles errors */ }
}

function addItem() {
  form.items.push({
    lineNo: form.items.length + 1, itemCode: '', itemDesc: '',
    uom: 'EA', qtyOrdered: undefined, unitPrice: undefined,
  })
}

function recalcLine() {
  // Preview: line amount shown in table (calculated on save)
}

function validateItems() {
  // Validation is done on submit
}

function openCreate() {
  isEdit.value = false
  resetForm()
  autoGeneratePoNo()
  addItem()
  formVisible.value = true
}

function openEdit(row: PurchaseOrderVO) {
  isEdit.value = true
  resetForm()
  form.id = row.id
  form.poNo = row.poNo
  form.cono = row.cono
  form.vendorCode = row.vendorCode || ''
  form.vendorName = row.vendorName || ''
  form.orderDate = row.orderDate || ''
  form.reqDate = row.reqDate || ''
  form.currency = row.currency
  form.notes = row.notes || ''
  // Load items
  getPurchaseOrderDetail(row.id).then(detail => {
    form.items = detail.items.map(i => ({
      lineNo: i.lineNo, itemCode: i.itemCode, itemDesc: i.itemDesc,
      uom: i.uom, qtyOrdered: i.qtyOrdered, unitPrice: i.unitPrice,
    }))
  })
  formVisible.value = true
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value && form.id) {
      await updatePurchaseOrder(form.id, form as PurchaseOrderUpdateDTO)
    } else {
      await createPurchaseOrder(form as PurchaseOrderCreateDTO)
    }
    ElMessage.success(t('common.saveSuccess'))
    formVisible.value = false
    search()
  } finally {
    saving.value = false
  }
}

// 详情
const detailVisible = ref(false)
const detail = ref<PurchaseOrderDetail | null>(null)
const detailTab = ref('info')

function openDetail(row: PurchaseOrderVO) {
  detailTab.value = 'info'
  getPurchaseOrderDetail(row.id).then(d => {
    detail.value = d
    detailVisible.value = true
  })
}

// 操作
async function handleSubmitApproval(id: number) {
  try {
    await ElMessageBox.confirm(t('procurement.approval.confirmApprove'), t('common.tip'), { type: 'info' })
  } catch { return }
  try {
    await submitForApproval(id)
    ElMessage.success(t('common.updateSuccess'))
    detailVisible.value = false
    search()
  } catch { /* interceptor handles errors */ }
}

async function handleReceive(id: number) {
  try {
    await ElMessageBox.confirm(t('procurement.po.receive'), t('common.tip'), { type: 'info' })
  } catch { return }
  try {
    await receiveOrder(id)
    ElMessage.success(t('common.updateSuccess'))
    detailVisible.value = false
    search()
  } catch { /* interceptor handles errors */ }
}

async function handleCancel(id: number) {
  try {
    await ElMessageBox.confirm(t('common.confirmDelete'), t('common.tip'), { type: 'warning' })
  } catch { return }
  try {
    await cancelOrder(id)
    ElMessage.success(t('common.updateSuccess'))
    detailVisible.value = false
    search()
  } catch { /* interceptor handles errors */ }
}

async function handleDelete(row: PurchaseOrderVO) {
  try {
    await ElMessageBox.confirm(t('common.confirmDelete'), t('common.tip'), { type: 'warning' })
  } catch { return }
  try {
    await deletePurchaseOrder(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    search()
  } catch { /* interceptor handles errors */ }
}

// 工具
const fmtMoney = (v: number | null | undefined) => (v != null ? formatMoney(v) : '—')

function statusTag(status: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    DRAFT: 'info', PENDING_APPROVAL: 'warning', APPROVED: 'success',
    SHIPPED: 'primary', RECEIVED: 'success', PAID: 'success', CANCELLED: 'danger',
  }
  return map[status] || 'info'
}

function approvalTag(action: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    APPROVED: 'success', REJECTED: 'danger', RETURNED: 'warning',
  }
  return map[action] || 'info'
}

onMounted(async () => {
  try {
    await search()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  }
})
</script>
