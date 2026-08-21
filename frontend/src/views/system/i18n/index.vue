<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-select v-model="lang" class="w-130" clearable :placeholder="$t('sysI18n.lang')">
        <el-option :label="$t('sysI18n.langZh')" value="zh-CN" />
        <el-option :label="$t('sysI18n.langEn')" value="en-US" />
      </el-select>
      <el-input
        v-model="keyword"
        :placeholder="$t('sysI18n.search')"
        clearable
        class="w-200"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon> {{ $t('common.search') }}
      </el-button>
      <el-button @click="resetSearch">{{ $t('common.reset') }}</el-button>
      <div class="flex-1" />
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> {{ $t('sysI18n.add') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedData" size="small" border stripe class="w-full">
        <el-table-column prop="lang" :label="$t('sysI18n.lang')" width="100" align="center">
          <template #default="{ row }: { row: I18nEntry }">
            <el-tag size="small">{{ row.lang }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="i18nKey" :label="$t('sysI18n.key')" width="260" show-overflow-tooltip />
        <el-table-column prop="text" :label="$t('sysI18n.text')" min-width="220" show-overflow-tooltip />
        <el-table-column :label="$t('common.operation')" width="140" fixed="right">
          <template #default="{ row }: { row: I18nEntry }">
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
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item :label="$t('sysI18n.lang')" prop="lang">
          <el-select v-model="form.lang" :disabled="isEdit" class="w-full">
            <el-option :label="$t('sysI18n.langZh')" value="zh-CN" />
            <el-option :label="$t('sysI18n.langEn')" value="en-US" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('sysI18n.key')" prop="i18nKey">
          <el-input v-model="form.i18nKey" :disabled="isEdit" placeholder="menu.xxx / common.xxx" />
        </el-form-item>
        <el-form-item :label="$t('sysI18n.text')" prop="text">
          <el-input v-model="form.text" type="textarea" :rows="3" />
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
import {
  listI18nEntries,
  saveI18nEntry,
  updateI18nEntry,
  deleteI18nEntry,
  type I18nEntry,
} from '@/api/i18n'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { useFormDialog } from '@/composables/useFormDialog'

defineOptions({ name: 'SysI18n' })

const { t } = useI18n()

const lang = ref<string>()

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
} = useSmartQueryTable<I18nEntry>({
  fetchApi: (params) => listI18nEntries({ ...params, lang: lang.value }),
  defaultSize: 20,
  buildParams: (base) => ({ ...base, lang: lang.value }),
})

interface I18nForm { i18nKey: string; lang: string; text: string }

const {
  dialogVisible,
  dialogTitle,
  isEdit,
  loading: submitLoading,
  formRef,
  form,
  rules: formRules,
  openCreate,
  openEdit,
  onSubmit,
} = useFormDialog<I18nForm>({
  defaultForm: () => ({ i18nKey: '', lang: 'zh-CN', text: '' }),
  rules: {
    lang: [{ required: true, message: t('sysI18n.langRequired'), trigger: 'change' }],
    i18nKey: [{ required: true, message: t('sysI18n.keyRequired'), trigger: 'blur' }],
    text: [{ required: true, message: t('sysI18n.textRequired'), trigger: 'blur' }],
  },
  createApi: (data) => saveI18nEntry(data),
  updateApi: (_id, data) => updateI18nEntry(data),
  onSuccess: () => handleRefresh(),
  i18nPrefix: 'sysI18n',
})

async function onDelete(row: I18nEntry) {
  try {
    await ElMessageBox.confirm(t('sysI18n.deleteConfirm', { key: row.i18nKey, lang: row.lang }), t('common.tip'), { type: 'warning' })
    await deleteI18nEntry(row.lang, row.i18nKey)
    ElMessage.success(t('common.deleteSuccess'))
    handleRefresh()
  } catch {
    /* cancelled */
  }
}
</script>