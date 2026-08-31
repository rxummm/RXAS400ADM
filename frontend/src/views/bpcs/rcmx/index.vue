<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="custLike" class="w-120" clearable :placeholder="$t('bpcs.common.customerCode')" @keyup.enter="load" />
      <el-input v-model="csrLike" class="w-120 ml8" clearable :placeholder="$t('bpcs.rcmx.csrId')" @keyup.enter="load" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="openAdd" class="ml8">{{ $t('common.add') }}</el-button>
      <el-button @click="handleImport" class="ml8">{{ $t('import') }}</el-button>
      <el-button @click="handleExport">{{ $t('common.export') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="cust" :label="$t('bpcs.common.customerCode')" width="100" />
        <el-table-column prop="custName" :label="$t('bpcs.common.customerName')" min-width="160" />
        <el-table-column prop="csrId" :label="$t('bpcs.rcmx.csrId')" width="100" />
        <el-table-column prop="csrName" :label="$t('bpcs.rcmx.csrName')" width="100" />
        <el-table-column prop="active" :label="$t('bpcs.common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(row as RcmxAssignment).active === 'Y' ? 'success' : 'info'" size="small">{{ (row as RcmxAssignment).active === 'Y' ? $t('enabled') : $t('disabled') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maintUser" :label="$t('bpcs.rcmx.maintUser')" width="100" />
        <el-table-column prop="maintDate" :label="$t('bpcs.rcmx.maintDate')" width="100" />
        <el-table-column :label="$t('common.actions')" width="160" align="center">
          <template #default="{ row }">
            <el-button size="small" link @click="openEdit(row as RcmxAssignment)">{{ $t('common.edit') }}</el-button>
            <el-button size="small" link type="danger" @click="confirmDelete(row as RcmxAssignment)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editMode ? $t('bpcs.rcmx.editTitle') : $t('bpcs.rcmx.addTitle')" width="500" destroy-on-close @close="resetForm">
      <el-form :model="form" label-width="100px" class="w-full">
        <el-form-item :label="$t('bpcs.common.customerCode')">
          <el-input v-model="form.cust" :placeholder="$t('common.keyword')" style="width: 100%" :disabled="editMode" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.rcmx.csrId')">
          <el-input v-model="form.csrId" :placeholder="$t('common.keyword')" style="width: 100%" :disabled="editMode" />
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
import { listRcmx, getRcmx, createRcmx, updateRcmx, deleteRcmx, importRcmx, exportRcmx, type RcmxAssignment } from '@/api/bpcs'
import { UploadFilled } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'

const { t } = useI18n()

const custLike = ref('')
const csrLike = ref('')
const loading = ref(false)
const rows = ref<RcmxAssignment[]>([])
const dialogVisible = ref(false)
const importVisible = ref(false)
const resultVisible = ref(false)
const editMode = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref({ successCount: 0, failureCount: 0, errors: [] as string[] })
const fileInputRef = ref<HTMLInputElement | null>(null)

const form = ref({
  cust: '',
  csrId: '',
  active: 'Y',
  maintUser: 'SYSTEM',
})

const load = async () => {
  loading.value = true
  try {
    rows.value = await listRcmx('001', custLike.value || undefined, csrLike.value || undefined) as unknown as RcmxAssignment[]
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  editMode.value = false
  form.value = { cust: '', csrId: '', active: 'Y', maintUser: 'SYSTEM' }
  dialogVisible.value = true
}

const openEdit = async (row: RcmxAssignment) => {
  editMode.value = true
  const res = await getRcmx('001', row.cust) as unknown as RcmxAssignment
  form.value = { ...res }
  dialogVisible.value = true
}

const resetForm = () => {
  form.value = { cust: '', csrId: '', active: 'Y', maintUser: 'SYSTEM' }
}

const submitForm = async () => {
  try {
    if (editMode.value) {
      await updateRcmx('001', form.value.cust, form.value)
    } else {
      await createRcmx('001', form.value)
    }
    ElMessage.success(editMode.value ? t('common.updateSuccess') : t('common.addSuccess'))
    dialogVisible.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || t('common.operationFailed'))
  }
}

const confirmDelete = (row: RcmxAssignment) => {
  ElMessageBox.confirm(t('common.confirmDelete'), t('common.tip'), { type: 'warning' })
    .then(async () => {
      try {
        await deleteRcmx('001', row.cust)
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
    importResult.value = await importRcmx('001', importFile.value) as unknown as { successCount: number; failureCount: number; errors: string[] }
    importVisible.value = false
    resultVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || t('common.importFailed'))
  }
}

const handleExport = async () => {
  try {
    const data = await exportRcmx('001') as unknown as RcmxAssignment[]
    const wsData = [[t('bpcs.common.customerCode'), t('bpcs.common.customerName'), t('bpcs.rcmx.csrId'), t('bpcs.rcmx.csrName'), t('common.status'), t('bpcs.rcmx.maintUser'), t('bpcs.rcmx.maintDate')], ...data.map(r => [r.cust, r.custName, r.csrId, r.csrName, r.active, r.maintUser, r.maintDate])]
    const wb = XLSX.utils.book_new()
    const ws = XLSX.utils.aoa_to_sheet(wsData)
    XLSX.utils.book_append_sheet(wb, ws, 'RCMX')
    XLSX.writeFile(wb, 'RCMX.xlsx')
  } catch (e: any) {
    ElMessage.error(t('common.exportFailed'))
  }
}

onMounted(load)
</script>