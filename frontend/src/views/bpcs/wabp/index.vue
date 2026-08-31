<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-select v-model="queryWh" placeholder="仓库" style="width: 120px" clearable>
        <el-option v-for="w in warehouses" :key="w" :label="w" :value="w" />
      </el-select>
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="openAdd">{{ $t('common.add') }}</el-button>
      <el-button @click="handleImport" class="ml8">{{ $t('import') }}</el-button>
      <el-button @click="handleExport">{{ $t('common.export') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="wh" :label="$t('bpcs.wms.warehouse')" width="90" />
        <el-table-column prop="dayOfWeek" :label="$t('bpcs.wabp.dayOfWeek')" width="90" align="center">
          <template #default="{ row }">{{ dayLabel((row as WabpConfig).dayOfWeek) }}</template>
        </el-table-column>
        <el-table-column prop="time" :label="$t('bpcs.wabp.time')" width="90" />
        <el-table-column prop="shipHold" :label="$t('bpcs.wabp.shipHold')" width="90" align="center" />
        <el-table-column prop="crHold" :label="$t('bpcs.wabp.crHold')" width="90" align="center" />
        <el-table-column prop="prHold" :label="$t('bpcs.wabp.prHold')" width="90" align="center" />
        <el-table-column prop="active" :label="$t('bpcs.common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(row as WabpConfig).active === 'Y' ? 'success' : 'info'" size="small">{{ (row as WabpConfig).active === 'Y' ? $t('enabled') : $t('disabled') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maintUser" :label="$t('bpcs.rcmx.maintUser')" width="100" />
        <el-table-column prop="maintDate" :label="$t('bpcs.rcmx.maintDate')" width="110" />
        <el-table-column :label="$t('common.actions')" width="160" align="center">
          <template #default="{ row }">
            <el-button size="small" link @click="openEdit(row as WabpConfig)">{{ $t('common.edit') }}</el-button>
            <el-button size="small" link type="danger" @click="confirmDelete(row as WabpConfig)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editMode ? $t('bpcs.wabp.editTitle') : $t('bpcs.wabp.addTitle')" width="500" destroy-on-close @close="resetForm">
      <el-form :model="form" label-width="120px" class="w-full">
        <el-form-item :label="$t('bpcs.wms.warehouse')">
          <el-select v-model="form.wh" :placeholder="$t('common.status')" style="width: 100%" :disabled="editMode">
            <el-option v-for="w in warehouses" :key="w" :label="w" :value="w" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('bpcs.wabp.dayOfWeek')">
          <el-select v-model="form.dayOfWeek" :placeholder="$t('common.status')" style="width: 100%" :disabled="editMode">
            <el-option v-for="d in dayOptions" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('bpcs.wabp.time')">
          <el-time-picker v-model="form.time" format="HH:mm:ss" value-format="HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.wabp.shipHold')">
          <el-select v-model="form.shipHold" :placeholder="$t('common.status')" style="width: 100%">
            <el-option :label="$t('common.yes')" value="Y" />
            <el-option :label="$t('common.no')" value="N" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('bpcs.wabp.crHold')">
          <el-select v-model="form.crHold" :placeholder="$t('common.status')" style="width: 100%">
            <el-option :label="$t('common.yes')" value="Y" />
            <el-option :label="$t('common.no')" value="N" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('bpcs.wabp.prHold')">
          <el-select v-model="form.prHold" :placeholder="$t('common.status')" style="width: 100%">
            <el-option :label="$t('common.yes')" value="Y" />
            <el-option :label="$t('common.no')" value="N" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('bpcs.common.status')">
          <el-select v-model="form.active" :placeholder="$t('common.status')" style="width: 100%">
            <el-option :label="$t('enabled')" value="Y" />
            <el-option :label="$t('disabled')" value="N" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submitForm">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog v-model="importVisible" :title="$t('import')" width="500">
      <div class="import-drop" @click="fileInputRef?.click()" @dragover.prevent @drop.prevent="handleDrop">
        <input ref="fileInputRef" type="file" accept=".xlsx,.xls" hidden @change="onFileChange" />
        <el-icon><UploadFilled /></el-icon>
        <p>{{ $t('common.clickOrDragUpload') }}</p>
        <p class="hint">{{ $t('common.xlsxOnly') }}</p>
        <p class="hint" v-if="importFile">{{ $t('common.fileSelected', { name: importFile.name }) }}</p>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :disabled="!importFile" @click="submitImport">{{ $t('common.confirmImport') }}</el-button>
      </template>
    </el-dialog>

    <!-- 导入结果对话框 -->
    <el-dialog v-model="resultVisible" :title="$t('importResult')" width="500">
      <p>{{ $t('common.importSuccess', { count: importResult.successCount }) }}</p>
      <p v-if="importResult.failureCount > 0" class="text-danger">{{ $t('common.importFailed', { count: importResult.failureCount }) }}</p>
      <el-collapse v-if="importResult.errors && importResult.errors.length > 0">
        <el-collapse-item :name="`error-${i}`" v-for="(err, i) in importResult.errors" :key="i" :title="$t('common.errorItem', { index: i + 1 })">
          {{ err }}
        </el-collapse-item>
      </el-collapse>
      <template #footer>
        <el-button type="primary" @click="resultVisible = false; load()">{{ $t('common.ok') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
import { listWabp, getWabp, createWabp, updateWabp, deleteWabp, importWabp, exportWabp, type WabpConfig } from '@/api/bpcs'
import { UploadFilled } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'

const loading = ref(false)
const rows = ref<WabpConfig[]>([])
const dialogVisible = ref(false)
const importVisible = ref(false)
const resultVisible = ref(false)
const editMode = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref({ successCount: 0, failureCount: 0, errors: [] as string[] })
const queryWh = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)

const warehouses = ['WH1', 'WH2', 'WH3', 'WH4']
const dayOptions = [
  { value: 1, label: 'Mon' }, { value: 2, label: 'Tue' },
  { value: 3, label: 'Wed' }, { value: 4, label: 'Thu' },
  { value: 5, label: 'Fri' }, { value: 6, label: 'Sat' },
  { value: 7, label: 'Sun' }
]

const form = ref({
  wh: '',
  dayOfWeek: 1,
  time: '14:00:00',
  shipHold: 'Y',
  crHold: 'N',
  prHold: 'N',
  active: 'Y',
  maintUser: 'SYSTEM',
})

const dayLabel = (d: number) => dayOptions.find(o => o.value === d)?.label || String(d)

const load = async () => {
  loading.value = true
  try {
    rows.value = await listWabp(queryWh.value || '001') as unknown as WabpConfig[]
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  editMode.value = false
  form.value = { wh: '', dayOfWeek: 1, time: '14:00:00', shipHold: 'Y', crHold: 'N', prHold: 'N', active: 'Y', maintUser: 'SYSTEM' }
  dialogVisible.value = true
}

const openEdit = async (row: WabpConfig) => {
  editMode.value = true
  const res = await getWabp('001', row.wh, row.dayOfWeek) as unknown as WabpConfig
  form.value = { ...res }
  dialogVisible.value = true
}

const resetForm = () => {
  form.value = { wh: '', dayOfWeek: 1, time: '14:00:00', shipHold: 'Y', crHold: 'N', prHold: 'N', active: 'Y', maintUser: 'SYSTEM' }
}

const submitForm = async () => {
  try {
    if (editMode.value) {
      await updateWabp('001', form.value.wh, form.value.dayOfWeek, form.value)
    } else {
      await createWabp('001', form.value)
    }
    ElMessage.success(editMode.value ? t('common.updateSuccess') : t('common.addSuccess'))
    dialogVisible.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || t('common.operationFailed'))
  }
}

const confirmDelete = (row: WabpConfig) => {
  ElMessageBox.confirm(t('common.confirmDelete'), t('common.tip'), { type: 'warning' })
    .then(async () => {
      try {
        await deleteWabp('001', row.wh, row.dayOfWeek)
        ElMessage.success(t('common.deleteSuccess'))
        load()
      } catch (e: any) {
        ElMessage.error(e.response?.data?.message || t('common.deleteFailed'))
      }
    })
}

const handleImport = () => {
  importFile.value = null
  importVisible.value = true
}

const onFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.files && target.files[0]) {
    importFile.value = target.files[0]
  }
}

const handleDrop = (e: DragEvent) => {
  if (e.dataTransfer?.files[0]) {
    importFile.value = e.dataTransfer.files[0]
  }
}

const submitImport = async () => {
  if (!importFile.value) return
  try {
    importResult.value = await importWabp('001', importFile.value) as unknown as { successCount: number; failureCount: number; errors: string[] }
    importVisible.value = false
    resultVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || t('common.importFailed'))
  }
}

const handleExport = async () => {
  try {
    const data = await exportWabp('001') as unknown as WabpConfig[]
    const wsData = [[t('bpcs.wms.warehouse'), t('bpcs.wabp.dayOfWeek'), t('bpcs.wabp.time'), 'SHPHOLD', 'CRHOLD', 'PRHOLD', t('common.status'), t('bpcs.rcmx.maintUser'), t('bpcs.rcmx.maintDate')], ...data.map(r => [r.wh, dayLabel(r.dayOfWeek), r.time, r.shipHold, r.crHold, r.prHold, r.active, r.maintUser, r.maintDate])]
    const wb = XLSX.utils.book_new()
    const ws = XLSX.utils.aoa_to_sheet(wsData)
    XLSX.utils.book_append_sheet(wb, ws, 'WABP')
    XLSX.writeFile(wb, 'WABP.xlsx')
  } catch (e: any) {
    ElMessage.error(t('common.exportFailed'))
  }
}

onMounted(load)
</script>