<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="keyword" clearable :placeholder="$t('common.keyword')" @keyup.enter="load" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="resetQuery">{{ $t('common.reset') }}</el-button>
      <el-button type="primary" @click="openAdd">{{ $t('common.add') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
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
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="load" />
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
import { listOrderTemplates, createOrderTemplate, updateOrderTemplate, deleteOrderTemplate, useOrderTemplate } from '@/api/bpcs'
import type { OrderTemplate } from '@/api/bpcs'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<OrderTemplate[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const keyword = ref('')

const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const form = ref<Partial<OrderTemplate>>({})

const dialogTitle = computed(() => editId.value ? t('common.edit') : t('common.add'))

const load = async () => {
  loading.value = true
  try {
    const res = await listOrderTemplates({ keyword: keyword.value || undefined })
    rows.value = res || []
    total.value = (res || []).length
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  keyword.value = ''
  current.value = 1
  load()
}

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
  load()
}

const handleDelete = async (row: OrderTemplate) => {
  await ElMessageBox.confirm(t('common.confirmDelete'), t('common.warning'), { type: 'warning' })
  await deleteOrderTemplate(row.id)
  ElMessage.success(t('common.deleteSuccess'))
  load()
}

const handleUse = async (row: OrderTemplate) => {
  await useOrderTemplate(row.id)
  ElMessage.success(t('common.operationSuccess'))
  load()
}

load()
</script>
