<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        :placeholder="$t('config.key')"
        clearable
        class="w-240"
        @keyup.enter="forceSearch"
      />
      <el-button type="primary" @click="onSearch">
        <el-icon><Search /></el-icon> {{ $t('common.search') }}
      </el-button>
      <el-button @click="resetSearch">{{ $t('common.reset') }}</el-button>
      <div class="flex-1" />
      <el-button v-has-perm="'SYS_CONFIG_MANAGE'" type="primary" @click="() => openCreate()">
        <el-icon><Plus /></el-icon> {{ $t('common.create') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedRows" size="small" border stripe class="w-full">
        <el-table-column prop="configKey" :label="$t('config.key')" width="280" show-overflow-tooltip />
        <el-table-column prop="configValue" :label="$t('config.value')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="description" :label="$t('config.description')" min-width="220" show-overflow-tooltip />
        <el-table-column :label="$t('common.operation')" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-has-perm="'SYS_CONFIG_MANAGE'" link type="primary" size="small" @click="openEdit(row as SysConfig)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button v-has-perm="'SYS_CONFIG_MANAGE'" link type="danger" size="small" @click="onDelete(row as SysConfig)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="() => {}" @size-change="onSizeChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" :close-on-click-modal="false">
      <el-form :model="form" label-width="90px">
        <el-form-item :label="$t('config.key')" prop="configKey">
          <el-input v-model="form.configKey" :disabled="isEdit" :placeholder="$t('config.keyHint')" />
        </el-form-item>
        <el-form-item :label="$t('config.value')">
          <el-input v-model="form.configValue" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="$t('config.description')">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listConfigs, updateConfig, deleteConfig, type SysConfig } from '@/api/config'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { useFormDialog } from '@/composables/useFormDialog'

defineOptions({ name: 'SysConfig' })

const { t } = useI18n()

const {
  tableData,
  keyword,
  loading,
  current,
  size,
  total,
  forceSearch,
  resetSearch: baseResetSearch,
} = useSmartQueryTable<SysConfig>({
  fetchApi: () => listConfigs().then((data) => data || []),
  frontendPage: true,
  enableCache: true,
  searchFields: ['configKey', 'description'],
})

const pagedRows = computed(() => tableData.value)

const onSizeChange = () => {
  current.value = 1
}

type ConfigForm = { configKey: string; configValue: string; description: string }

const {
  dialogVisible,
  dialogTitle,
  isEdit,
  loading: saving,
  form,
  openCreate,
  openEdit,
  onSubmit,
} = useFormDialog<ConfigForm>({
  defaultForm: () => ({ configKey: '', configValue: '', description: '' }),
  saveApi: async (_isEdit, data) => {
    await updateConfig(data.configKey, { configValue: data.configValue, description: data.description })
  },
  onSuccess: () => forceSearch(),
  i18nPrefix: 'config',
  validate: false,
})

const resetSearch = () => {
  baseResetSearch()
}

const onSearch = () => {
  forceSearch()
}

async function onDelete(row: SysConfig) {
  try {
    await ElMessageBox.confirm(t('config.deleteConfirm', { key: row.configKey }), t('common.tip'), { type: 'warning' })
    await deleteConfig(row.configKey)
    ElMessage.success(t('common.deleteSuccess'))
    forceSearch()
  } catch {
    /* cancelled */
  }
}
</script>
