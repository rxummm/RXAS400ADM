<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="keyword" clearable :placeholder="$t('common.keyword')" @keyup.enter="forceSearch" />
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
      <el-button @click="resetSearch">{{ $t('common.reset') }}</el-button>
      <el-button type="primary" @click="openAdd">{{ $t('common.add') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="templateName" :label="$t('bpcs.orderTemplate.templateName')" min-width="200" />
        <el-table-column prop="cono" :label="$t('bpcs.common.companyCode')" width="100" />
        <el-table-column prop="cust" :label="$t('bpcs.common.customerCode')" width="100" />
        <el-table-column prop="useCount" :label="$t('bpcs.orderTemplate.useCount')" width="100" align="center" />
        <el-table-column :label="$t('common.operation')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="openEdit(row as OrderTemplate)">{{ $t('common.edit') }}</el-button>
            <el-button size="small" link type="primary" @click="handleUse(row as OrderTemplate)">{{ $t('common.use') }}</el-button>
            <el-button size="small" link type="danger" @click="handleDelete(row as OrderTemplate)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item :label="$t('bpcs.orderTemplate.templateName')" required>
          <el-input v-model="form.templateName" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.common.companyCode')">
          <el-input v-model="form.cono" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.common.customerCode')">
          <el-input v-model="form.cust" class="w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsOrderTemplate' })

import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { listOrderTemplates, createOrderTemplate, updateOrderTemplate, deleteOrderTemplate, useOrderTemplate } from '@/api/bpcs'
import type { OrderTemplate } from '@/api/bpcs'

const { t } = useI18n()

const { tableData, keyword, loading, total, current, size, forceSearch, resetSearch, handlePageChange, handleSizeChange, fetchData } = useSmartQueryTable<OrderTemplate>({
  fetchApi: (params) => listOrderTemplates({ keyword: params.keyword, current: params.current, size: params.size }),
  frontendPage: false,
  searchFields: ['templateName', 'cono', 'cust'],
})

const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const form = ref<Partial<OrderTemplate>>({})

const dialogTitle = computed(() => editId.value ? t('common.edit') : t('common.add'))

const openAdd = () => {
  editId.value = null
  form.value = {}
  dialogVisible.value = true
}

const openEdit = (row: OrderTemplate) => {
  editId.value = row.id
  form.value = { ...row }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.templateName) {
    ElMessage.warning(t('common.validation.notBlank'))
    return
  }
  if (editId.value) {
    await updateOrderTemplate({ ...form.value, id: editId.value })
    ElMessage.success(t('common.updateSuccess'))
  } else {
    await createOrderTemplate(form.value)
    ElMessage.success(t('common.addSuccess'))
  }
  dialogVisible.value = false
  void fetchData()
}

const handleDelete = async (row: OrderTemplate) => {
  try {
    await ElMessageBox.confirm(t('common.confirmDelete'), t('common.warning'), { type: 'warning' })
  } catch {
    return
  }
  await deleteOrderTemplate(row.id)
  ElMessage.success(t('common.deleteSuccess'))
  void fetchData()
}

const handleUse = async (row: OrderTemplate) => {
  await useOrderTemplate(row.id)
  ElMessage.success(t('common.operationSuccess'))
  void fetchData()
}
</script>
