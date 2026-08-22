<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-select v-model="type" :placeholder="$t('ipRules.type')" clearable class="w-130">
        <el-option :label="$t('ipRules.black')" value="BLACK" />
        <el-option :label="$t('ipRules.white')" value="WHITE" />
      </el-select>
      <el-input
        v-model="keyword"
        :placeholder="$t('ipRules.ip')"
        clearable
        class="w-200"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon> {{ $t('common.search') }}
      </el-button>
      <el-button @click="resetSearch">{{ $t('common.reset') }}</el-button>
      <div class="flex-1" />
      <el-button type="primary" @click="() => openCreate()">
        <el-icon><Plus /></el-icon> {{ $t('ipRules.add') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedData" size="small" border stripe class="w-full">
        <el-table-column prop="ip" :label="$t('ipRules.ip')" width="180" />
        <el-table-column prop="type" :label="$t('ipRules.type')" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'BLACK' ? 'danger' : 'success'">
              {{ row.type === 'BLACK' ? $t('ipRules.black') : $t('ipRules.white') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('ipRules.description')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="enabled" :label="$t('ipRules.enabled')" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? $t('common.yes') : $t('common.no') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size"
        @change="handlePageChange" @size-change="handleSizeChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item :label="$t('ipRules.ip')" prop="ip">
          <el-input v-model="form.ip" :placeholder="$t('ipRules.ipHint')" />
        </el-form-item>
        <el-form-item :label="$t('ipRules.type')" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="BLACK">{{ $t('ipRules.black') }}</el-radio>
            <el-radio value="WHITE">{{ $t('ipRules.white') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('ipRules.description')">
          <el-input v-model="form.description" />
        </el-form-item>
        <el-form-item :label="$t('ipRules.enabled')">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="onSubmit">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listIpRules, createIpRule, updateIpRule, deleteIpRule, type IpRule } from '@/api/ipRule'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { useFormDialog } from '@/composables/useFormDialog'

defineOptions({ name: 'IpRules' })

const { t } = useI18n()

const type = ref<string>()

const {
  pagedData,
  keyword,
  loading,
  total,
  current,
  size,
  handleSearch,
  resetSearch,
  handlePageChange,
  handleSizeChange,
  handleRefresh,
} = useSmartQueryTable<IpRule>({
  fetchApi: (params) => listIpRules({ ...params, type: type.value }),
  defaultSize: 10,
  buildParams: (base) => ({ ...base, type: type.value }),
})

interface IpRuleForm { id?: number; ip: string; type: 'BLACK' | 'WHITE'; description: string; enabled: number }

const {
  dialogVisible,
  dialogTitle,
  loading: submitLoading,
  formRef,
  form,
  rules: formRules,
  openCreate,
  openEdit,
  onSubmit,
} = useFormDialog<IpRuleForm>({
  defaultForm: () => ({ id: undefined, ip: '', type: 'BLACK', description: '', enabled: 1 }),
  rules: {
    ip: [{ required: true, message: () => t('ipRules.ipRequired'), trigger: 'blur' }],
    type: [{ required: true, message: () => t('ipRules.typeRequired'), trigger: 'change' }],
  },
  createApi: (data) => createIpRule(data),
  updateApi: (id, data) => updateIpRule(Number(id), data),
  onSuccess: () => handleRefresh(),
  i18nPrefix: 'ipRules',
})

async function onDelete(row: IpRule) {
  try {
    await ElMessageBox.confirm(t('ipRules.deleteConfirm'), t('common.tip'), { type: 'warning' })
    if (row.id) await deleteIpRule(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    handleRefresh()
  } catch {
    /* cancelled */
  }
}
</script>