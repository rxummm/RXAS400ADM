<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="itemCode" :placeholder="$t('quality.itemCode')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-select v-model="status" :placeholder="$t('quality.status')" clearable class="search-bar__select">
        <el-option label="OPEN" value="OPEN" />
        <el-option label="IN_PROGRESS" value="IN_PROGRESS" />
        <el-option label="CLOSED" value="CLOSED" />
      </el-select>
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="ncrNo" :label="$t('quality.ncrNo')" width="140" />
        <el-table-column prop="itemCode" :label="$t('quality.itemCode')" width="120" />
        <el-table-column prop="itemDesc" :label="$t('quality.itemDesc')" />
        <el-table-column prop="batchNo" :label="$t('quality.batchNo')" width="100" />
        <el-table-column prop="qtyRejected" :label="$t('quality.qtyRejected')" width="100" />
        <el-table-column prop="defectType" :label="$t('quality.defectType')" width="120" />
        <el-table-column prop="disposition" :label="$t('quality.disposition')" width="120" />
        <el-table-column prop="status" :label="$t('quality.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OPEN' ? 'danger' : row.status === 'IN_PROGRESS' ? 'warning' : 'success'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignedTo" :label="$t('quality.assignedTo')" width="100" />
        <el-table-column prop="dueDate" :label="$t('quality.dueDate')" width="110" />
        <el-table-column :label="$t('common.action')" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row as Ncr)">{{ $t('common.update') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
      <el-form ref="formRef" :model="dialogForm" label-width="80px">
        <el-form-item :label="$t('quality.status')" prop="status">
          <el-select v-model="dialogForm.status" class="w-full">
            <el-option label="OPEN" value="OPEN" />
            <el-option label="IN_PROGRESS" value="IN_PROGRESS" />
            <el-option label="CLOSED" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('quality.closeRemark')" prop="closeRemark" v-if="dialogForm.status === 'CLOSED'">
          <el-input v-model="dialogForm.closeRemark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="loading" @click="onSubmit">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'QualityNcr' })

import { ref, onMounted } from 'vue'
import { searchNcr, updateNcrStatus, type Ncr } from '@/api/quality'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { useFormDialog } from '@/composables/useFormDialog'
import AppPagination from '@/components/AppPagination.vue'

const itemCode = ref('')
const status = ref('')

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<Ncr>({
  fetchApi: (params) => searchNcr({
    itemCode: itemCode.value,
    status: status.value,
    current: params.current,
    size: params.size,
  }),
})

const { dialogVisible, dialogTitle, formRef, form: dialogForm, openEdit, onSubmit } =
  useFormDialog<Ncr & { closeRemark: string }>({
    defaultForm: () => ({ id: 0, status: 'OPEN', closeRemark: '' } as Ncr & { closeRemark: string }),
    saveApi: async (_isEdit, data) => {
      await updateNcrStatus(data.id, data.status, data.closeRemark)
    },
    onSuccess: () => forceSearch(),
    validate: false,
  })

onMounted(() => forceSearch())
</script>