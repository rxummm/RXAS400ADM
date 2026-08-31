<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="showCreateDialog = true">
        {{ $t('bpcs.cycleCount.createPlan') }}
      </el-button>
      <el-select v-model="statusFilter" class="w-120" :placeholder="$t('common.status')" clearable @change="loadPlans">
        <el-option label="PENDING" value="PENDING" />
        <el-option label="IN_PROGRESS" value="IN_PROGRESS" />
        <el-option label="COMPLETED" value="COMPLETED" />
      </el-select>
      <el-button @click="loadPlans">{{ $t('common.search') }}</el-button>
    </div>

    <div class="table-wrapper">
      <el-table :data="plans" v-loading="loading" size="small" border>
        <el-table-column prop="planNo" :label="$t('bpcs.cycleCount.planNo')" width="130" />
        <el-table-column prop="item" :label="$t('bpcs.label.item')" min-width="120" />
        <el-table-column prop="itemDesc" :label="$t('bpcs.label.itemDesc')" min-width="150" show-overflow-tooltip />
        <el-table-column prop="warehouse" :label="$t('bpcs.cycleCount.warehouse')" width="90" />
        <el-table-column prop="plannedDate" :label="$t('bpcs.cycleCount.plannedDate')" width="110" />
        <el-table-column prop="status" :label="$t('common.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="abcClass" :label="$t('bpcs.abcXyz.abcClass')" width="70" align="center" />
        <el-table-column :label="$t('common.operation')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING' || row.status === 'IN_PROGRESS'" type="primary" link size="small"
              @click="openRecordDialog(row as CycleCountPlan)">
              {{ $t('bpcs.cycleCount.record') }}
            </el-button>
            <el-button type="info" link size="small" @click="viewResults(row as CycleCountPlan)">
              {{ $t('bpcs.cycleCount.results') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 创建计划弹窗 -->
    <el-dialog v-model="showCreateDialog" :title="$t('bpcs.cycleCount.createPlan')" width="500px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item :label="$t('bpcs.label.item')" prop="item">
          <el-input v-model="createForm.item" maxlength="15" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.label.itemDesc')">
          <el-input v-model="createForm.itemDesc" maxlength="50" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.cycleCount.warehouse')" prop="warehouse">
          <el-input v-model="createForm.warehouse" maxlength="4" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.cycleCount.plannedDate')" prop="plannedDate">
          <el-date-picker v-model="createForm.plannedDate" type="date" value-format="YYYY-MM-DD" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.abcXyz.abcClass')">
          <el-select v-model="createForm.abcClass" clearable>
            <el-option label="A" value="A" />
            <el-option label="B" value="B" />
            <el-option label="C" value="C" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 录入结果弹窗 -->
    <el-dialog v-model="showRecordDialog" :title="$t('bpcs.cycleCount.recordTitle')" width="400px" :close-on-click-modal="false">
      <el-form :model="recordForm" label-width="100px">
        <el-form-item :label="$t('bpcs.cycleCount.systemQty')">
          <el-input-number v-model="recordForm.systemQty" :min="0" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.cycleCount.countedQty')">
          <el-input-number v-model="recordForm.countedQty" :min="0" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.cycleCount.reason')">
          <el-input v-model="recordForm.reason" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRecordDialog = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="recording" @click="handleRecord">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'CycleCount' })

import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  createCycleCountPlan, listCycleCountPlans, recordCycleCountResult,
  type CycleCountPlan
} from '@/api/bpcs'

const { t } = useI18n()
const loading = ref(false)
const creating = ref(false)
const recording = ref(false)
const plans = ref<CycleCountPlan[]>([])
const statusFilter = ref('')

const showCreateDialog = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ item: '', itemDesc: '', warehouse: '', plannedDate: '', abcClass: '' })
const createRules: FormRules = {
  item: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
  warehouse: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
  plannedDate: [{ required: true, message: t('common.validation.notNull'), trigger: 'change' }]
}

const showRecordDialog = ref(false)
const recordForm = reactive({ planId: 0, systemQty: 0, countedQty: 0, reason: '' })

async function loadPlans() {
  loading.value = true
  try {
    plans.value = await listCycleCountPlans({ status: statusFilter.value || undefined, limit: 50 })
  } catch {
    /* interceptor handles error */
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    await createCycleCountPlan(createForm)
    ElMessage.success(t('common.addSuccess'))
    showCreateDialog.value = false
    loadPlans()
  } catch {
    /* interceptor handles error */
  } finally {
    creating.value = false
  }
}

function openRecordDialog(plan: CycleCountPlan) {
  recordForm.planId = plan.id
  recordForm.systemQty = 0
  recordForm.countedQty = 0
  recordForm.reason = ''
  showRecordDialog.value = true
}

async function handleRecord() {
  recording.value = true
  try {
    await recordCycleCountResult({ planId: recordForm.planId, countedQty: recordForm.countedQty, reason: recordForm.reason }, recordForm.systemQty)
    ElMessage.success(t('common.addSuccess'))
    showRecordDialog.value = false
    loadPlans()
  } catch {
    /* interceptor handles error */
  } finally {
    recording.value = false
  }
}

function viewResults(plan: CycleCountPlan) {
  ElMessage.info(t('bpcs.cycleCount.results') + ': ' + plan.planNo)
}

function statusType(status: string) {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    COMPLETED: 'success', IN_PROGRESS: 'warning', PENDING: 'info', CANCELLED: 'danger'
  }
  return map[status] || 'info'
}

onMounted(loadPlans)
</script>
