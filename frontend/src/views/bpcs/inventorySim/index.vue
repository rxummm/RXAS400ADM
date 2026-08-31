<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="query.itemNo" class="w-160" :placeholder="$t('bpcs.simulation.itemNo')" clearable @keyup.enter="load" />
      <el-input v-model="query.warehouse" class="w-120" :placeholder="$t('bpcs.simulation.warehouse')" clearable @keyup.enter="load" />
      <el-select v-model="query.status" class="w-120" :placeholder="$t('bpcs.simulation.status')" clearable>
        <el-option :label="$t('bpcs.simulation.draft')" value="DRAFT" />
        <el-option :label="$t('bpcs.simulation.running')" value="RUNNING" />
        <el-option :label="$t('bpcs.simulation.completed')" value="COMPLETED" />
      </el-select>
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="resetQuery">{{ $t('common.reset') }}</el-button>
      <el-button type="primary" @click="openCreateDialog">{{ $t('bpcs.simulation.addSimulation') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="simName" :label="$t('bpcs.simulation.simName')" min-width="120" />
        <el-table-column prop="itemNo" :label="$t('bpcs.simulation.itemNo')" width="100" />
        <el-table-column prop="warehouse" :label="$t('bpcs.simulation.warehouse')" width="80" />
        <el-table-column prop="currentStock" :label="$t('bpcs.simulation.currentStock')" width="90" align="right" />
        <el-table-column prop="demandChange" :label="$t('bpcs.simulation.demandChange')" width="100" align="right" />
        <el-table-column prop="leadTimeDays" :label="$t('bpcs.simulation.leadTimeDays')" width="90" align="right" />
        <el-table-column prop="resultStockoutDays" :label="$t('bpcs.simulation.resultStockoutDays')" width="110" align="right">
          <template #default="{ row }">
            <span :class="(row as InventorySimulationVO).resultStockoutDays > 0 ? 'text-danger' : ''">
              {{ (row as InventorySimulationVO).resultStockoutDays }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="resultReorderCount" :label="$t('bpcs.simulation.resultReorderCount')" width="110" align="right" />
        <el-table-column prop="resultAvgStock" :label="$t('bpcs.simulation.resultAvgStock')" width="100" align="right" />
        <el-table-column prop="resultServiceLevel" :label="$t('bpcs.simulation.resultServiceLevel')" width="100" align="right">
          <template #default="{ row }">
            <span :class="(row as InventorySimulationVO).resultServiceLevel < 95 ? 'text-danger' : 'text-success'">
              {{ (row as InventorySimulationVO).resultServiceLevel }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('bpcs.simulation.status')" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType((row as InventorySimulationVO).status)" size="small">
              {{ $t('bpcs.simulation.' + (row as InventorySimulationVO).status.toLowerCase()) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="(row as InventorySimulationVO).status === 'DRAFT'" size="small" link type="primary" @click="handleRun(row as InventorySimulationVO)">{{ $t('bpcs.simulation.run') }}</el-button>
            <el-button size="small" link type="danger" @click="handleDelete(row as InventorySimulationVO)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination :total="total" v-model:current="page.current" v-model:size="page.size" @change="load" @size-change="load" />
    </div>

    <!-- ==================== 创建模拟弹窗 ==================== -->
    <el-dialog v-model="dialogVisible" :title="$t('bpcs.simulation.addSimulation')" width="520px" destroy-on-close>
      <el-form :model="form" label-width="110" ref="formRef" :rules="formRules">
        <el-form-item :label="$t('bpcs.simulation.simName')" prop="simName">
          <el-input v-model="form.simName" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.simulation.itemNo')" prop="itemNo">
          <el-input v-model="form.itemNo" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.simulation.warehouse')" prop="warehouse">
          <el-input v-model="form.warehouse" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.simulation.currentStock')" prop="currentStock">
          <el-input-number v-model="form.currentStock" :min="0" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.simulation.demandChange')">
          <el-input-number v-model="form.demandChange" :min="1" :max="500" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.simulation.leadTimeDays')">
          <el-input-number v-model="form.leadTimeDays" :min="1" :max="365" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.simulation.safetyStock')">
          <el-input-number v-model="form.safetyStock" :min="0" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.simulation.reorderPoint')">
          <el-input-number v-model="form.reorderPoint" :min="0" class="w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">{{ $t('common.save') }}</el-button>
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
import { listSimulations, createSimulation, runSimulation, deleteSimulation, type InventorySimulationVO } from '@/api/bpcs'
import AppPagination from '@/components/AppPagination.vue'

defineOptions({ name: 'BpcsInventorySim' })

const loading = ref(false)
const rows = ref<InventorySimulationVO[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 20 })
const query = reactive({ itemNo: '', warehouse: '', status: '' })

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ simName: '', itemNo: '', warehouse: '', currentStock: 0, demandChange: 100, leadTimeDays: 7, safetyStock: 0, reorderPoint: 0 })
const formRules = reactive<FormRules>({
  simName: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
  itemNo: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
  warehouse: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
  currentStock: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
})

function statusTagType(status: string) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'RUNNING') return 'warning'
  return 'info'
}

async function load() {
  loading.value = true
  try {
    const params: Record<string, string | number> = { current: page.current, size: page.size }
    if (query.itemNo) params.itemNo = query.itemNo
    if (query.warehouse) params.warehouse = query.warehouse
    if (query.status) params.status = query.status
    const res = await listSimulations(params) as unknown as { records: InventorySimulationVO[]; total: number }
    rows.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

function resetQuery() {
  query.itemNo = ''
  query.warehouse = ''
  query.status = ''
  page.current = 1
  load()
}

function openCreateDialog() {
  Object.assign(form, { simName: '', itemNo: '', warehouse: '', currentStock: 0, demandChange: 100, leadTimeDays: 7, safetyStock: 0, reorderPoint: 0 })
  dialogVisible.value = true
}

async function handleCreate() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    await createSimulation(form)
    dialogVisible.value = false
    ElMessage.success('OK')
    load()
  } finally { saving.value = false }
}

async function handleRun(row: InventorySimulationVO) {
  await ElMessageBox.confirm(`Run simulation "${row.simName}"?`, '', { type: 'info' })
  await runSimulation(row.id)
  ElMessage.success('OK')
  load()
}

async function handleDelete(row: InventorySimulationVO) {
  await ElMessageBox.confirm(`Delete simulation "${row.simName}"?`, '', { type: 'warning' })
  await deleteSimulation(row.id)
  ElMessage.success('OK')
  load()
}

onMounted(load)
</script>
