<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        :placeholder="$t('permissions.searchPlaceholder')"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-select v-model="moduleFilter" :placeholder="$t('permissions.module')" clearable>
        <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleSearch">{{ $t('common.search') }}</el-button>
      <el-button @click="resetSearch">{{ $t('common.reset') }}</el-button>
      <div class="flex-1" />
      <el-button type="primary" :icon="Plus" v-has-perm="'PERMISSION_MANAGE'" @click="() => openCreate()">
        {{ $t('permissions.create') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedData" size="small" border stripe>
        <el-table-column prop="permissionCode" :label="$t('permissions.code')" width="220" fixed="left">
          <template #default="{ row }">
            <el-tag size="small" type="warning">{{ row.permissionCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="permissionName" :label="$t('permissions.name')" min-width="150" show-overflow-tooltip />
        <el-table-column prop="module" :label="$t('permissions.module')" width="160">
          <template #default="{ row }">
            <el-tag v-if="row.module" size="small" type="info">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('permissions.description')" min-width="200" show-overflow-tooltip />
        <el-table-column :label="$t('permissions.usage')" width="180">
          <template #default="{ row }">
            <span :class="{ 'text-danger': (row.menuUsage || 0) + (row.roleUsage || 0) > 0 }">
              {{ $t('permissions.usageDetail', { menu: row.menuUsage || 0, role: row.roleUsage || 0 }) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-has-perm="'PERMISSION_MANAGE'" link type="primary" size="small" @click="openEdit(row)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button
              v-has-perm="'PERMISSION_MANAGE'"
              link
              type="danger"
              size="small"
              :disabled="(row.menuUsage || 0) + (row.roleUsage || 0) > 0"
              :loading="removeLoading === row.id"
              @click="confirmRemove(row)"
            >
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size"
        @change="handlePageChange" @size-change="handleSizeChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="var(--rx-dialog-sm)" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="var(--rx-form-label-width)">
        <el-form-item :label="$t('permissions.code')" prop="permissionCode">
          <el-input v-model="form.permissionCode" :disabled="isEdit" placeholder="JOB_END" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('permissions.name')" prop="permissionName">
          <el-input v-model="form.permissionName" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('permissions.module')">
          <el-input v-model="form.module" :placeholder="$t('permissions.moduleHint')" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('permissions.description')">
          <el-input v-model="form.description" type="textarea" :rows="2" class="w-full" />
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
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirmDelete } from '@/composables/useConfirmDelete'
import { Plus, Search } from '@element-plus/icons-vue'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { useFormDialog } from '@/composables/useFormDialog'
import {
  createPermissionCode,
  deletePermissionCode,
  listAllPermissionCodes,
  listPermissionCodes,
  updatePermissionCode,
  type PermissionCode,
} from '@/api/permission'

defineOptions({ name: 'SysPermissions' })

const { t } = useI18n()

const moduleFilter = ref<string | null>(null)
const modules = ref<string[]>([])

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
} = useSmartQueryTable<PermissionCode>({
  fetchApi: (params) => listPermissionCodes({
    ...params,
    module: moduleFilter.value || undefined,
  }),
  defaultSize: 20,
  buildParams: (base) => ({ ...base, module: moduleFilter.value || undefined }),
})

interface PermissionForm { id?: number; permissionCode: string; permissionName: string; module: string; description: string }

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
} = useFormDialog<PermissionForm>({
  defaultForm: () => ({ id: undefined, permissionCode: '', permissionName: '', module: '', description: '' }),
  rules: {
    permissionCode: [{ required: true, message: () => t('permissions.codeRequired'), trigger: 'blur' }],
  },
  saveApi: async (isEditVal, data) => {
    if (isEditVal && data.id) {
      await updatePermissionCode(data.id, {
        permissionName: data.permissionName,
        module: data.module,
        description: data.description,
      })
    } else {
      await createPermissionCode({
        permissionCode: data.permissionCode,
        permissionName: data.permissionName,
        module: data.module,
        description: data.description,
      })
    }
  },
  onSuccess: () => { handleRefresh(); loadModules(); },
  i18nPrefix: 'permissions',
})

async function loadModules() {
  try {
    const all = await listAllPermissionCodes()
    const mods = Array.from(new Set(all.map((p) => p.module))).filter((m): m is string => !!m)
    modules.value = mods.sort()
  } catch {
    /* interceptor 已提示错误 */
  }
}

const { removeLoading, confirmRemove } = useConfirmDelete({
  deleteApi: (row: PermissionCode) => deletePermissionCode(row.id!),
  onSuccess: () => { handleRefresh(); loadModules() },
  confirmMessage: 'permissions.deleteConfirm',
  confirmTitle: 'common.tip',
})

onMounted(loadModules)
</script>