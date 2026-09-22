<template>
  <div class="page-container page-container--fit">
    <!-- 搜索区 -->
    <div class="search-bar">
      <el-input v-model="query.invoiceNo" class="w-150" :placeholder="$t('ar.invoice.invoiceNo')" clearable @keyup.enter="search" />
      <el-input v-model="query.customerName" class="w-150" :placeholder="$t('ar.invoice.customerName')" clearable @keyup.enter="search" />
      <el-select v-model="query.status" :placeholder="$t('ar.invoice.status')" clearable class="w-130">
        <el-option v-for="s in arStatusOptions" :key="s.value" :label="$t(s.label)" :value="s.value" />
      </el-select>
      <el-date-picker v-model="query.invoiceDateFrom" type="date" :placeholder="$t('common.startDate')" class="w-140" />
      <el-date-picker v-model="query.invoiceDateTo" type="date" :placeholder="$t('common.endDate')" class="w-140" />
      <el-date-picker v-model="query.dueDateFrom" type="date" :placeholder="$t('ar.invoice.dueDate')" class="w-140" />
      <el-date-picker v-model="query.dueDateTo" type="date" :placeholder="$t('ar.invoice.dueDate')" class="w-140" />
      <el-button type="primary" :loading="loading" @click="search">{{ $t('common.search') }}</el-button>
      <el-button @click="reset">{{ $t('common.reset') }}</el-button>
      <div class="flex-1" />
      <el-button type="primary" @click="openCreate" v-if="hasPerm('AR_MANAGE')">
        <el-icon><Plus /></el-icon>{{ $t('ar.invoice.create') }}
      </el-button>
    </div>

    <!-- 表格 -->
    <div class="table-wrapper">
      <el-table :data="invoices" v-loading="loading" size="small" border highlight-current-row @row-click="openDetail">
        <el-table-column prop="invoiceNo" :label="$t('ar.invoice.invoiceNo')" min-width="140" />
        <el-table-column prop="customerName" :label="$t('ar.invoice.customerName')" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.customerName || row.customerCode }}</template>
        </el-table-column>
        <el-table-column prop="invoiceDate" :label="$t('ar.invoice.invoiceDate')" width="110" />
        <el-table-column prop="dueDate" :label="$t('ar.invoice.dueDate')" width="110">
          <template #default="{ row }">
            <span :class="row.dueDate && isOverdue(row as ArInvoiceVO) ? 'text-danger' : ''">{{ row.dueDate }}</span>
          </template>
        </el-table-column>
        <el-table-column align="right" :label="$t('ar.invoice.totalAmount')" width="120">
          <template #default="{ row }">{{ fmtMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('ar.invoice.paidAmount')" width="110">
          <template #default="{ row }">{{ fmtMoney(row.paidAmount) }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('ar.invoice.balance')" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="balanceTag(row.balance)">{{ fmtMoney(row.balance) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('ar.invoice.status')" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ $t(row.statusKey) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('ar.invoice.agingBucket')" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.agingBucket" size="small" :type="agingTag(row.agingBucket)">{{ $t('ar.aging' + row.agingBucket) || row.agingBucket }}</el-tag>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" :label="$t('ar.invoice.createdBy')" width="100" />
        <el-table-column prop="createdTime" :label="$t('ar.invoice.createdTime')" width="160">
          <template #default="{ row }">{{ row.createdTime?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row as ArInvoiceVO)">{{ $t('common.detail') }}</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="primary" size="small" @click.stop="openEdit(row as ArInvoiceVO)">{{ $t('ar.invoice.edit') }}</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="danger" size="small" @click.stop="handleDelete(row as ArInvoiceVO)">{{ $t('common.delete') }}</el-button>
            <el-button v-if="row.status === 'OPEN' || row.status === 'PARTIAL'" link type="success" size="small" @click.stop="openReceive(row as ArInvoiceVO)">{{ $t('ar.invoice.receive') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-if="total > 0" :total="total" v-model:current="current" v-model:size="size" @change="search" @size-change="search" />
      <el-empty v-if="searched && !loading && invoices.length === 0" :description="$t('ar.noInvoice')" />
    </div>

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="formVisible" :title="isEdit ? $t('ar.invoice.edit') : $t('ar.invoice.create')" width="720px" :close-on-click-modal="false" top="5vh">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px" size="small" v-loading="saving">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('ar.invoice.invoiceNo')" prop="invoiceNo">
              <el-input v-model="form.invoiceNo" :disabled="isEdit">
                <template #append v-if="!isEdit">
                  <el-button @click="autoGenerateInvoiceNo">{{ $t('common.refresh') }}</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('ar.invoice.currency')" prop="currency">
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
            <el-form-item :label="$t('ar.invoice.customerCode')" prop="customerCode">
              <el-input v-model="form.customerCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('ar.invoice.customerName')">
              <el-input v-model="form.customerName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="$t('ar.invoice.invoiceDate')" prop="invoiceDate">
              <el-date-picker v-model="form.invoiceDate" type="date" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('ar.invoice.dueDate')" prop="dueDate">
              <el-date-picker v-model="form.dueDate" type="date" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider>{{ $t('ar.invoice.amountSection') }}</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item :label="$t('ar.invoice.subtotal')">
              <el-input-number v-model="form.subtotal" :min="0" :precision="2" class="w-full" @change="recalcFromSubtotal" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('ar.invoice.taxRate')">
              <el-input-number v-model="form.taxRate" :min="0" :max="1" :precision="4" class="w-full" @change="recalcFromSubtotal" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="$t('ar.invoice.taxAmount')">
              <el-input v-model="form.taxAmount" :disabled="true" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('ar.invoice.totalAmount')" class="mb16">
          <el-input v-model="form.totalAmount" :disabled="true" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('ar.invoice.originPoNo')">
          <el-input v-model="form.originPoNo" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('ar.invoice.notes')" class="mt16">
          <el-input v-model="form.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 收款对话框 -->
    <el-dialog v-model="receiveVisible" :title="$t('ar.invoice.receive')" width="520px" :close-on-click-modal="false">
      <el-form ref="receiveFormRef" :model="receiveForm" :rules="receiveRules" label-width="110px" size="small" v-loading="saving">
        <el-form-item :label="$t('ar.invoice.invoiceNo')">
          <el-tag>{{ receiveForm.invoiceNo }}</el-tag>
        </el-form-item>
        <el-form-item :label="$t('ar.invoice.balance')">
          <el-tag type="warning">{{ fmtMoney(receiveForm.balance) }}</el-tag>
        </el-form-item>
        <el-form-item :label="$t('ar.payment.paymentNo')" prop="paymentNo">
          <el-input v-model="receiveForm.paymentNo">
            <template #append>
              <el-button @click="autoGeneratePaymentNo">{{ $t('common.refresh') }}</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="$t('ar.payment.paymentDate')" prop="paymentDate">
          <el-date-picker v-model="receiveForm.paymentDate" type="date" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('ar.payment.amount')" prop="amount">
          <el-input-number v-model="receiveForm.amount" :min="0" :precision="2" :max="receiveForm.balance" class="w-full" @change="onAmountChange" />
        </el-form-item>
        <el-form-item :label="$t('ar.payment.paymentMethod')" prop="paymentMethod">
          <el-select v-model="receiveForm.paymentMethod" class="w-full">
            <el-option v-for="m in paymentMethods" :key="m.value" :label="$t(m.label)" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('ar.payment.referenceNo')">
          <el-input v-model="receiveForm.referenceNo" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('ar.payment.receivedBy')">
          <el-input v-model="receiveForm.receivedBy" class="w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="receiveVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="confirmReceive">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="$t('ar.invoice.detail') + ' · ' + (detail?.invoiceNo || '')" size="720px" :close-on-click-modal="false">
      <template v-if="detail">
        <el-tabs v-model="detailTab">
          <el-tab-pane :label="$t('ar.invoice.detail')" name="info">
            <el-descriptions :column="2" size="small" border>
              <el-descriptions-item :label="$t('ar.invoice.invoiceNo')">{{ detail.invoiceNo }}</el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.status')">
                <el-tag size="small" :type="statusTag(detail.status)">{{ $t(detail.statusKey) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.customerCode')">{{ detail.customerCode || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.customerName')">{{ detail.customerName || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.invoiceDate')">{{ detail.invoiceDate || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.dueDate')">{{ detail.dueDate || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.subtotal')">{{ fmtMoney(detail.subtotal) }}</el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.taxRate')">{{ detail.taxRate != null ? (detail.taxRate * 100).toFixed(2) + '%' : '—' }}</el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.taxAmount')">{{ fmtMoney(detail.taxAmount) }}</el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.totalAmount')">{{ fmtMoney(detail.totalAmount) }}</el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.paidAmount')">{{ fmtMoney(detail.paidAmount) }}</el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.balance')">
                <el-tag size="small" :type="balanceTag(detail.balance)">{{ fmtMoney(detail.balance) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.currency')">{{ detail.currency }}</el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.agingBucket')">
                <el-tag v-if="detail.agingBucket" size="small" :type="agingTag(detail.agingBucket!)">{{ $t('ar.aging' + detail.agingBucket) || detail.agingBucket }}</el-tag>
                <span v-else class="text-muted">—</span>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.createdBy')" :span="1">{{ detail.createdBy }}</el-descriptions-item>
              <el-descriptions-item :label="$t('ar.invoice.createdTime')" :span="1">{{ detail.createdTime?.replace('T', ' ') }}</el-descriptions-item>
            </el-descriptions>
            <div v-if="detail.notes" class="mt8">
              <strong>{{ $t('ar.invoice.notes') }}：</strong>{{ detail.notes }}
            </div>
            <div class="mt16">
              <el-button v-if="detail.status === 'DRAFT'" type="primary" size="small" @click="handleEditFromDetail(detail.id)">
                {{ $t('ar.invoice.edit') }}
              </el-button>
              <el-button v-if="detail.status === 'OPEN' || detail.status === 'PARTIAL'" type="success" size="small" @click="openReceiveFromDetail(detail)">
                {{ $t('ar.invoice.receive') }}
              </el-button>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="$t('ar.invoice.payments')" name="payments">
            <el-table :data="detail.payments" size="small" border>
              <el-table-column prop="paymentNo" :label="$t('ar.payment.paymentNo')" width="140" />
              <el-table-column prop="paymentDate" :label="$t('ar.payment.paymentDate')" width="120" />
              <el-table-column align="right" :label="$t('ar.payment.amount')" width="110">
                <template #default="{ row }">{{ fmtMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column prop="referenceNo" :label="$t('ar.payment.referenceNo')" show-overflow-tooltip />
              <el-table-column prop="receivedBy" :label="$t('ar.payment.receivedBy')" width="100" />
              <el-table-column prop="createdBy" :label="$t('common.createdBy')" width="100" />
              <el-table-column prop="createdTime" :label="$t('common.createdTime')" width="160">
                <template #default="{ row }">{{ row.createdTime?.replace('T', ' ') }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="detail.payments.length === 0" :description="$t('common.noData')" />
          </el-tab-pane>

          <el-tab-pane :label="$t('ar.invoice.aging')" name="aging">
            <div class="aging-report">
              <el-card shadow="never">
                <template #header>
                  <div class="section">{{ $t('ar.agingTitle') }}</div>
                </template>
                <el-table :data="agingRows" size="small" border class="w-full">
                  <el-table-column :label="$t('ar.invoice.customerName')" min-width="160" show-overflow-tooltip>
                    <template #default="{ row }">{{ row.customerName || row.customerCode }}</template>
                  </el-table-column>
                  <el-table-column :label="$t('ar.agingCurrent')" width="130" align="right">
                    <template #default="{ row }">{{ fmtMoney(row.current) }}</template>
                  </el-table-column>
                  <el-table-column :label="$t('ar.aging0_30')" width="130" align="right">
                    <template #default="{ row }">{{ fmtMoney(row.aging0_30) }}</template>
                  </el-table-column>
                  <el-table-column :label="$t('ar.aging31_60')" width="130" align="right">
                    <template #default="{ row }">{{ fmtMoney(row.aging31_60) }}</template>
                  </el-table-column>
                  <el-table-column :label="$t('ar.aging61_90')" width="130" align="right">
                    <template #default="{ row }">{{ fmtMoney(row.aging61_90) }}</template>
                  </el-table-column>
                  <el-table-column :label="$t('ar.aging90plus')" width="130" align="right">
                    <template #default="{ row }">{{ fmtMoney(row.aging90plus) }}</template>
                  </el-table-column>
                  <el-table-column :label="$t('ar.agingTotal')" width="130" align="right">
                    <template #default="{ row }">{{ fmtMoney(row.total) }}</template>
                  </el-table-column>
                </el-table>
                <el-empty v-if="agingRows.length === 0" :description="$t('common.noData')" />
              </el-card>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'FinanceAr' })

import { reactive, ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  pageArInvoices,
  getArInvoiceDetail,
  generateArInvoiceNo,
  generateArPaymentNo,
  createArInvoice,
  updateArInvoice,
  deleteArInvoice,
  recordArPayment,
  type ArInvoiceVO,
  type ArInvoiceDetail,
  type ArInvoiceCreateDTO,
  type ArInvoiceUpdateDTO,
  type ArInvoiceQuery,
  type ArPaymentCreateDTO,
} from '@/api/ar'
import AppPagination from '@/components/AppPagination.vue'
import { formatMoney } from '@/utils/format'
import type { FormInstance, FormRules } from 'element-plus'

const { t } = useI18n()
const userStore = useUserStore()
const hasPerm = (code: string) => userStore.canSeeTab('ar', code)

// AR 状态选项
const arStatusOptions = [
  { value: '', label: 'common.all' },
  { value: 'DRAFT', label: 'ar.statusDraft' },
  { value: 'OPEN', label: 'ar.statusOpen' },
  { value: 'PARTIAL', label: 'ar.statusPartial' },
  { value: 'PAID', label: 'ar.statusPaid' },
  { value: 'OVERDUE', label: 'ar.statusOverdue' },
  { value: 'WRITE_OFF', label: 'ar.statusWriteOff' },
]

const loading = ref(false)
const searched = ref(false)
const invoices = ref<ArInvoiceVO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const query = reactive<ArInvoiceQuery>({
  invoiceNo: '', customerName: '', status: '', overdue: undefined,
  invoiceDateFrom: '', invoiceDateTo: '', dueDateFrom: '', dueDateTo: '',
})

function search() {
  loading.value = true
  searched.value = true
  pageArInvoices({ current: current.value, size: size.value, ...query })
    .then(data => { invoices.value = data.records; total.value = data.total })
    .finally(() => { loading.value = false })
}

function reset() {
  query.invoiceNo = ''
  query.customerName = ''
  query.status = ''
  query.overdue = undefined
  query.invoiceDateFrom = ''
  query.invoiceDateTo = ''
  query.dueDateFrom = ''
  query.dueDateTo = ''
  current.value = 1
  search()
}

function isOverdue(row: ArInvoiceVO) {
  if (!row.dueDate) return false
  const due = new Date(row.dueDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return due < today && row.balance > 0
}

// 表单
const formVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<ArInvoiceCreateDTO & { id?: number }>({
  invoiceNo: '', customerCode: '', customerName: '',
  invoiceDate: '', dueDate: '', originPoNo: '', originSoNo: '',
  totalAmount: 0, subtotal: undefined, taxRate: 0.13, taxAmount: undefined,
  currency: 'CNY', notes: '',
})

const formRules: FormRules = {
  customerCode: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
  invoiceDate: [{ required: true, message: t('common.validation.notNull'), trigger: 'change' }],
  dueDate: [{ required: true, message: t('common.validation.notNull'), trigger: 'change' }],
  totalAmount: [{ required: true, message: t('common.validation.notNull'), trigger: 'change' }],
}

function resetForm() {
  form.id = undefined
  form.invoiceNo = ''
  form.customerCode = ''
  form.customerName = ''
  form.invoiceDate = ''
  form.dueDate = ''
  form.originPoNo = ''
  form.originSoNo = ''
  form.totalAmount = 0
  form.subtotal = undefined
  form.taxRate = 0.13
  form.taxAmount = undefined
  form.currency = 'CNY'
  form.notes = ''
}

function recalcFromSubtotal() {
  const sub = form.subtotal ?? 0
  const rate = form.taxRate ?? 0
  form.taxAmount = sub * rate
  form.totalAmount = sub + form.taxAmount
}

function autoGenerateInvoiceNo() {
  generateArInvoiceNo().then(no => { form.invoiceNo = no }).catch(() => {})
}

function openCreate() {
  isEdit.value = false
  resetForm()
  autoGenerateInvoiceNo()
  formVisible.value = true
}

function openEdit(row: ArInvoiceVO) {
  isEdit.value = true
  resetForm()
  form.id = row.id
  form.invoiceNo = row.invoiceNo
  form.customerCode = row.customerCode || ''
  form.customerName = row.customerName || ''
  form.invoiceDate = row.invoiceDate || ''
  form.dueDate = row.dueDate || ''
  form.originPoNo = row.originPoNo || ''
  form.originSoNo = row.originSoNo || ''
  form.totalAmount = row.totalAmount
  form.subtotal = row.subtotal ?? undefined
  form.taxRate = row.taxRate ?? 0.13
  form.taxAmount = row.taxAmount ?? undefined
  form.currency = row.currency
  form.notes = row.notes || ''
  recalcFromSubtotal()
  formVisible.value = true
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value && form.id) {
      await updateArInvoice(form.id, form as ArInvoiceUpdateDTO)
    } else {
      await createArInvoice(form as ArInvoiceCreateDTO)
    }
    ElMessage.success(t('common.saveSuccess'))
    formVisible.value = false
    search()
  } finally {
    saving.value = false
  }
}

// 收款
const receiveVisible = ref(false)
const receiveFormRef = ref<FormInstance>()
const receiveForm = reactive<ArPaymentCreateDTO & { invoiceNo: string; balance: number }>({
  invoiceId: 0, paymentNo: '', paymentDate: '', amount: 0,
  paymentMethod: 'BANK_TRANSFER', referenceNo: '', receivedBy: '', notes: '',
  invoiceNo: '', balance: 0,
})

const receiveRules: FormRules = {
  paymentNo: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
  paymentDate: [{ required: true, message: t('common.validation.notNull'), trigger: 'change' }],
  amount: [{ required: true, message: t('common.validation.notNull'), trigger: 'change' },
           { validator: (rule, val, cb) => {
             if (val != null && val > receiveForm.balance) cb(new Error(t('common.validation.max')))
             else cb()
           }}],
  paymentMethod: [{ required: true, message: t('common.validation.notBlank'), trigger: 'change' }],
}

const paymentMethods = [
  { value: 'BANK_TRANSFER', label: 'ar.paymentMethod.bankTransfer' },
  { value: 'CASH', label: 'ar.paymentMethod.cash' },
  { value: 'CHECK', label: 'ar.paymentMethod.check' },
  { value: 'CREDIT_CARD', label: 'ar.paymentMethod.creditCard' },
  { value: 'OTHER', label: 'ar.paymentMethod.other' },
]

function autoGeneratePaymentNo() {
  generateArPaymentNo().then(no => { receiveForm.paymentNo = no }).catch(() => {})
}

function onAmountChange() {
  if (receiveForm.amount > receiveForm.balance) {
    receiveForm.amount = receiveForm.balance
  }
}

function openReceive(row: ArInvoiceVO) {
  receiveForm.invoiceId = row.id
  receiveForm.invoiceNo = row.invoiceNo
  receiveForm.balance = row.balance
  receiveForm.paymentDate = new Date().toISOString().slice(0, 10)
  receiveForm.amount = row.balance
  receiveForm.paymentMethod = 'BANK_TRANSFER'
  receiveForm.paymentNo = ''
  receiveForm.referenceNo = ''
  receiveForm.receivedBy = ''
  autoGeneratePaymentNo()
  receiveVisible.value = true
}

async function confirmReceive() {
  const valid = await receiveFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await recordArPayment(receiveForm.invoiceId, receiveForm as ArPaymentCreateDTO)
    ElMessage.success(t('common.updateSuccess'))
    receiveVisible.value = false
    search()
  } finally {
    saving.value = false
  }
}

// 详情
const detailVisible = ref(false)
const detail = ref<ArInvoiceDetail | null>(null)
const detailTab = ref('info')

function openDetail(row: ArInvoiceVO) {
  detailTab.value = 'info'
  getArInvoiceDetail(row.id).then(d => {
    detail.value = d
    detailVisible.value = true
  }).catch(() => {})
}

function handleEditFromDetail(id: number) {
  detailVisible.value = false
  const row = invoices.value.find(r => r.id === id)
  if (row) openEdit(row)
}

function openReceiveFromDetail(d: ArInvoiceDetail) {
  detailVisible.value = false
  openReceive({ ...d, id: d.id } as ArInvoiceVO)
}

// 删除
async function handleDelete(row: ArInvoiceVO) {
  try {
    await ElMessageBox.confirm(t('common.confirmDelete'), t('common.tip'), { type: 'warning' })
  } catch { return }
  try {
    await deleteArInvoice(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    search()
  } catch { /* interceptor */ }
}

// 账龄报表 tab 数据
const agingRows = ref<Array<{ customerCode: string; customerName?: string; current?: number; aging0_30?: number; aging31_60?: number; aging61_90?: number; aging90plus?: number; total?: number }>>([])

watch(detail, (d) => {
  if (!d || d.agingSummary?.total == null || d.agingSummary.total === 0) {
    agingRows.value = []
    return
  }
  agingRows.value = [{
    customerCode: d.customerCode,
    customerName: d.customerName,
    current: d.agingSummary.current ?? 0,
    aging0_30: d.agingSummary.aging0_30 ?? 0,
    aging31_60: d.agingSummary.aging31_60 ?? 0,
    aging61_90: d.agingSummary.aging61_90 ?? 0,
    aging90plus: d.agingSummary.aging90plus ?? 0,
    total: d.agingSummary.total ?? 0,
  }]
}, { immediate: true, deep: true })

// 工具
const fmtMoney = (v: number | null | undefined) => (v != null ? formatMoney(v) : '—')

function statusTag(status: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' | undefined {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    DRAFT: 'info', OPEN: 'warning', PARTIAL: 'warning',
    PAID: 'success', OVERDUE: 'danger', WRITE_OFF: 'info',
  }
  return map[status] || undefined
}

function balanceTag(balance: number): 'primary' | 'success' | 'warning' | 'info' | 'danger' | undefined {
  if (balance === 0) return 'success'
  return 'warning'
}

function agingTag(bucket: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' | undefined {
  if (bucket === 'OVERDUE' || bucket === '90+') return 'danger'
  if (bucket === '61-90' || bucket === '31-60') return 'warning'
  if (bucket === '0-30') return 'info'
  return undefined
}

onMounted(async () => {
  try {
    await search()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  }
})
</script>

<style scoped>
.aging-report {
  padding: 8px 0;
}
</style>
