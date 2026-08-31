<template>
  <div class="page-container page-container--fit">
    <el-tabs v-model="activeTab">
      <!-- ==================== 运费规则 Tab ==================== -->
      <el-tab-pane :label="$t('bpcs.freight.rules')" name="rules">
        <div class="search-bar">
          <el-button type="primary" @click="openRuleDialog()">{{ $t('bpcs.freight.addRule') }}</el-button>
        </div>
        <div class="table-wrapper">
          <el-table :data="rules" v-loading="rulesLoading" size="small" border>
            <el-table-column prop="ruleName" :label="$t('bpcs.freight.ruleName')" min-width="120" />
            <el-table-column prop="carrier" :label="$t('bpcs.freight.carrier')" width="100" />
            <el-table-column prop="costType" :label="$t('bpcs.freight.costType')" width="100" />
            <el-table-column prop="basePrice" :label="$t('bpcs.freight.basePrice')" width="100" align="right" />
            <el-table-column prop="unitPrice" :label="$t('bpcs.freight.unitPrice')" width="100" align="right" />
            <el-table-column prop="minPrice" :label="$t('bpcs.freight.minPrice')" width="100" align="right" />
            <el-table-column prop="maxPrice" :label="$t('bpcs.freight.maxPrice')" width="100" align="right" />
            <el-table-column :label="$t('bpcs.freight.enabled')" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="(row as FreightCostRuleVO).enabled ? 'success' : 'info'" size="small">
                  {{ (row as FreightCostRuleVO).enabled ? $t('common.yes') : $t('common.no') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="$t('common.actions')" width="140" align="center" fixed="right">
              <template #default="{ row }">
                <el-button size="small" link @click="openRuleDialog(row as FreightCostRuleVO)">{{ $t('common.edit') }}</el-button>
                <el-button size="small" link type="danger" @click="handleDeleteRule(row as FreightCostRuleVO)">{{ $t('common.delete') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ==================== 运费记录 Tab ==================== -->
      <el-tab-pane :label="$t('bpcs.freight.records')" name="records">
        <div class="search-bar">
          <el-input v-model="recordQuery.orderNo" class="w-160" :placeholder="$t('bpcs.freight.orderNo')" clearable @keyup.enter="loadRecords" />
          <el-input v-model="recordQuery.carrier" class="w-120" :placeholder="$t('bpcs.freight.carrier')" clearable @keyup.enter="loadRecords" />
          <el-button type="primary" @click="loadRecords">{{ $t('common.search') }}</el-button>
          <el-button @click="resetRecordQuery">{{ $t('common.reset') }}</el-button>
          <el-button type="primary" @click="openRecordDialog()">{{ $t('bpcs.freight.addRecord') }}</el-button>
        </div>
        <div class="table-wrapper">
          <el-table :data="records" v-loading="recordsLoading" size="small" border>
            <el-table-column prop="orderNo" :label="$t('bpcs.freight.orderNo')" width="120" />
            <el-table-column prop="carrier" :label="$t('bpcs.freight.carrier')" width="100" />
            <el-table-column prop="weight" :label="$t('bpcs.freight.weight')" width="90" align="right" />
            <el-table-column prop="volume" :label="$t('bpcs.freight.volume')" width="90" align="right" />
            <el-table-column prop="pieceCount" :label="$t('bpcs.freight.pieceCount')" width="70" align="right" />
            <el-table-column prop="estimatedCost" :label="$t('bpcs.freight.estimatedCost')" width="110" align="right" />
            <el-table-column prop="actualCost" :label="$t('bpcs.freight.actualCost')" width="110" align="right" />
            <el-table-column prop="costDiff" :label="$t('bpcs.freight.costDiff')" width="100" align="right">
              <template #default="{ row }">
                <span :class="(row as FreightCostRecordVO).costDiff > 0 ? 'text-danger' : ''">
                  {{ (row as FreightCostRecordVO).costDiff }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="shipDate" :label="$t('bpcs.freight.shipDate')" width="110" />
            <el-table-column :label="$t('common.actions')" width="80" align="center" fixed="right">
              <template #default="{ row }">
                <el-button size="small" link type="danger" @click="handleDeleteRecord(row as FreightCostRecordVO)">{{ $t('common.delete') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <AppPagination :total="recordTotal" v-model:current="recordPage.current" v-model:size="recordPage.size" @change="loadRecords" @size-change="loadRecords" />
        </div>
      </el-tab-pane>

      <!-- ==================== 成本分析 Tab ==================== -->
      <el-tab-pane :label="$t('bpcs.freight.analysis')" name="analysis">
        <div class="search-bar">
          <el-input v-model="trendCarrier" class="w-120" :placeholder="$t('bpcs.freight.carrier')" clearable @keyup.enter="loadTrend" />
          <el-button type="primary" @click="loadTrend">{{ $t('common.search') }}</el-button>
        </div>
        <div class="table-wrapper">
          <el-table :data="trends" v-loading="trendLoading" size="small" border>
            <el-table-column prop="month" :label="$t('bpcs.freight.monthlyTrend')" width="100" />
            <el-table-column prop="carrier" :label="$t('bpcs.freight.carrier')" width="100" />
            <el-table-column prop="totalCost" :label="$t('bpcs.freight.totalCost')" width="120" align="right" />
            <el-table-column prop="avgCost" :label="$t('bpcs.freight.avgCost')" width="120" align="right" />
            <el-table-column prop="recordCount" :label="$t('bpcs.freight.recordCount')" width="80" align="right" />
          </el-table>
        </div>
        <div class="mt16">
          <h4 class="section">{{ $t('bpcs.freight.carrierShare') }}</h4>
          <el-table :data="carrierShareList" size="small" border>
            <el-table-column prop="carrier" :label="$t('bpcs.freight.carrier')" width="150" />
            <el-table-column prop="cost" :label="$t('bpcs.freight.totalCost')" width="150" align="right" />
            <el-table-column prop="share" :label="$t('bpcs.freight.analysis')" width="100" align="right" />
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ==================== 规则新增/编辑弹窗 ==================== -->
    <el-dialog v-model="ruleDialogVisible" :title="ruleForm.id ? $t('common.edit') : $t('bpcs.freight.addRule')" width="520px" destroy-on-close>
      <el-form :model="ruleForm" label-width="100" ref="ruleFormRef" :rules="ruleRules">
        <el-form-item :label="$t('bpcs.freight.ruleName')" prop="ruleName">
          <el-input v-model="ruleForm.ruleName" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.carrier')" prop="carrier">
          <el-input v-model="ruleForm.carrier" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.costType')" prop="costType">
          <el-select v-model="ruleForm.costType" class="w-full">
            <el-option label="WEIGHT" value="WEIGHT" />
            <el-option label="VOLUME" value="VOLUME" />
            <el-option label="PIECE" value="PIECE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.basePrice')" prop="basePrice">
          <el-input-number v-model="ruleForm.basePrice" :min="0" :precision="2" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.unitPrice')" prop="unitPrice">
          <el-input-number v-model="ruleForm.unitPrice" :min="0" :precision="2" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.minPrice')">
          <el-input-number v-model="ruleForm.minPrice" :min="0" :precision="2" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.maxPrice')">
          <el-input-number v-model="ruleForm.maxPrice" :min="0" :precision="2" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.description')">
          <el-input v-model="ruleForm.description" type="textarea" class="w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="ruleSaving" @click="saveRule">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 记录新增弹窗 ==================== -->
    <el-dialog v-model="recordDialogVisible" :title="$t('bpcs.freight.addRecord')" width="520px" destroy-on-close>
      <el-form :model="recordForm" label-width="100" ref="recordFormRef" :rules="recordRules">
        <el-form-item :label="$t('bpcs.freight.orderNo')" prop="orderNo">
          <el-input v-model="recordForm.orderNo" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.carrier')" prop="carrier">
          <el-input v-model="recordForm.carrier" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.weight')">
          <el-input-number v-model="recordForm.weight" :min="0" :precision="2" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.volume')">
          <el-input-number v-model="recordForm.volume" :min="0" :precision="2" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.pieceCount')">
          <el-input-number v-model="recordForm.pieceCount" :min="0" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.estimatedCost')">
          <el-input-number v-model="recordForm.estimatedCost" :min="0" :precision="2" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.actualCost')">
          <el-input-number v-model="recordForm.actualCost" :min="0" :precision="2" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.freight.shipDate')">
          <el-date-picker v-model="recordForm.shipDate" type="date" value-format="YYYY-MM-DD" class="w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="recordSaving" @click="saveRecord">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
import {
  listFreightRules, createFreightRule, updateFreightRule, deleteFreightRule,
  listFreightRecords, createFreightRecord, deleteFreightRecord,
  getFreightTrend, getCarrierCostShare,
  type FreightCostRuleVO, type FreightCostRecordVO, type FreightCostTrendVO
} from '@/api/bpcs'
import AppPagination from '@/components/AppPagination.vue'

defineOptions({ name: 'BpcsFreightCost' })

const activeTab = ref('rules')

// ==================== 规则 ====================
const rulesLoading = ref(false)
const rules = ref<FreightCostRuleVO[]>([])
const ruleDialogVisible = ref(false)
const ruleSaving = ref(false)
const ruleFormRef = ref<FormInstance>()
const ruleForm = reactive<Partial<FreightCostRuleVO>>({})
const ruleRules = reactive<FormRules>({
  ruleName: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
  carrier: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
  costType: [{ required: true, message: t('common.validation.notBlank'), trigger: 'change' }],
  basePrice: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
  unitPrice: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
})

async function loadRules() {
  rulesLoading.value = true
  try { rules.value = await listFreightRules() as unknown as FreightCostRuleVO[] }
  finally { rulesLoading.value = false }
}

function openRuleDialog(row?: FreightCostRuleVO) {
  Object.assign(ruleForm, row ? { ...row } : { ruleName: '', carrier: '', costType: 'WEIGHT', basePrice: 0, unitPrice: 0, minPrice: 0, maxPrice: 0, enabled: true, description: '' })
  ruleDialogVisible.value = true
}

async function saveRule() {
  if (!ruleFormRef.value) return
  await ruleFormRef.value.validate()
  ruleSaving.value = true
  try {
    if (ruleForm.id) {
      await updateFreightRule(ruleForm.id, ruleForm)
    } else {
      await createFreightRule(ruleForm)
    }
    ruleDialogVisible.value = false
    ElMessage.success('OK')
    loadRules()
  } finally { ruleSaving.value = false }
}

async function handleDeleteRule(row: FreightCostRuleVO) {
  await ElMessageBox.confirm(`Delete rule "${row.ruleName}"?`, '', { type: 'warning' })
  await deleteFreightRule(row.id)
  ElMessage.success('OK')
  loadRules()
}

// ==================== 记录 ====================
const recordsLoading = ref(false)
const records = ref<FreightCostRecordVO[]>([])
const recordTotal = ref(0)
const recordPage = reactive({ current: 1, size: 20 })
const recordQuery = reactive({ orderNo: '', carrier: '' })
const recordDialogVisible = ref(false)
const recordSaving = ref(false)
const recordFormRef = ref<FormInstance>()
const recordForm = reactive<Partial<FreightCostRecordVO>>({})
const recordRules = reactive<FormRules>({
  orderNo: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
  carrier: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
})

async function loadRecords() {
  recordsLoading.value = true
  try {
    const params: Record<string, string | number> = { current: recordPage.current, size: recordPage.size }
    if (recordQuery.orderNo) params.orderNo = recordQuery.orderNo
    if (recordQuery.carrier) params.carrier = recordQuery.carrier
    const res = await listFreightRecords(params) as unknown as { records: FreightCostRecordVO[]; total: number }
    records.value = res.records
    recordTotal.value = res.total
  } finally { recordsLoading.value = false }
}

function resetRecordQuery() {
  recordQuery.orderNo = ''
  recordQuery.carrier = ''
  recordPage.current = 1
  loadRecords()
}

function openRecordDialog() {
  Object.assign(recordForm, { orderNo: '', carrier: '', weight: 0, volume: 0, pieceCount: 0, estimatedCost: 0, actualCost: 0, shipDate: '' })
  recordDialogVisible.value = true
}

async function saveRecord() {
  if (!recordFormRef.value) return
  await recordFormRef.value.validate()
  recordSaving.value = true
  try {
    await createFreightRecord(recordForm)
    recordDialogVisible.value = false
    ElMessage.success('OK')
    loadRecords()
  } finally { recordSaving.value = false }
}

async function handleDeleteRecord(row: FreightCostRecordVO) {
  await ElMessageBox.confirm(`Delete record "${row.orderNo}"?`, '', { type: 'warning' })
  await deleteFreightRecord(row.id)
  ElMessage.success('OK')
  loadRecords()
}

// ==================== 成本分析 ====================
const trendLoading = ref(false)
const trends = ref<FreightCostTrendVO[]>([])
const trendCarrier = ref('')
const carrierShareList = ref<{ carrier: string; cost: number; share: string }[]>([])

async function loadTrend() {
  trendLoading.value = true
  try {
    const params: Record<string, string | number> = { months: 12 }
    if (trendCarrier.value) params.carrier = trendCarrier.value
    trends.value = await getFreightTrend(params) as unknown as FreightCostTrendVO[]
  } finally { trendLoading.value = false }
}

async function loadCarrierShare() {
  const share = await getCarrierCostShare() as unknown as Record<string, number>
  const total = Object.values(share).reduce((a, b) => a + (b as number), 0) as number
  carrierShareList.value = Object.entries(share).map(([carrier, cost]) => ({
    carrier,
    cost: cost as number,
    share: total > 0 ? ((cost as number) / total * 100).toFixed(1) + '%' : '0%'
  }))
}

onMounted(() => { loadRules(); loadRecords(); loadTrend(); loadCarrierShare() })
</script>
