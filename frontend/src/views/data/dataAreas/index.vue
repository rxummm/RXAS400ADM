<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="library"
        class="w-150"
        :placeholder="$t('dataArea.library')"
        clearable
        @keyup.enter="load"
      />
      <el-button type="primary" :loading="loading" @click="load">
        {{ $t('common.search') }}
      </el-button>
      <el-button v-has-perm="'SYSVAL_EDIT'" type="success" @click="openCreate">
        {{ $t('common.create') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <el-table :data="areas" v-loading="loading" size="small" border>
        <el-table-column prop="DATA_AREA_LIBRARY" :label="$t('dataArea.library')" width="120" />
        <el-table-column prop="DATA_AREA_NAME" :label="$t('dataArea.name')" min-width="150" />
        <el-table-column prop="DATA_AREA_TYPE" :label="$t('dataArea.type')" width="100" />
        <el-table-column prop="DATA_AREA_LENGTH" :label="$t('dataArea.length')" width="90" />
        <el-table-column prop="DATA_AREA_VALUE" :label="$t('dataArea.value')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="DATA_AREA_DESCRIPTION" :label="$t('dataArea.description')" min-width="150" show-overflow-tooltip />
        <el-table-column :label="$t('common.operation')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row as DataArea)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button
              v-has-perm="'SYSVAL_EDIT'"
              size="small"
              type="danger"
              plain
              :loading="removeLoading === row.DATA_AREA_NAME"
              @click="handleDelete(row as DataArea)"
            >
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && areas.length === 0" :description="$t('common.noData')" />
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="editVisible"
      :title="$t('dataArea.editTitle')"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form v-if="editForm" label-width="100px">
        <el-form-item :label="$t('dataArea.library')">
          <el-input v-model="editForm.library" disabled />
        </el-form-item>
        <el-form-item :label="$t('dataArea.name')">
          <el-input v-model="editForm.name" disabled />
        </el-form-item>
        <el-form-item :label="$t('dataArea.type')">
          <el-input v-model="editForm.type" disabled />
        </el-form-item>
        <el-form-item :label="$t('dataArea.value')">
          <el-input v-model="editForm.value" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ $t('common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 创建弹窗 -->
    <el-dialog
      v-model="createVisible"
      :title="$t('dataArea.createTitle')"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px">
        <el-form-item :label="$t('dataArea.library')">
          <el-input v-model="createForm.library" />
        </el-form-item>
        <el-form-item :label="$t('dataArea.name')">
          <el-input v-model="createForm.name" />
        </el-form-item>
        <el-form-item :label="$t('dataArea.length')">
          <el-input-number v-model="createForm.length" :min="1" :max="2000" />
        </el-form-item>
        <el-form-item :label="$t('dataArea.value')">
          <el-input v-model="createForm.value" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">
          {{ $t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'DataAreas' })

import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  fetchDataAreas,
  updateDataArea,
  createDataArea,
  deleteDataArea,
  type DataArea,
} from '@/api/dataAreas'

const { t } = useI18n()

const library = ref('QSYS')
const loading = ref(false)
const areas = ref<DataArea[]>([])
const removeLoading = ref<string | null>(null)

const editVisible = ref(false)
const editForm = ref<{ library: string; name: string; type: string; value: string } | null>(null)
const createVisible = ref(false)
const createForm = ref({ library: 'QSYS', name: '', length: 50, value: '' })
const saving = ref(false)

function load() {
  loading.value = true
  fetchDataAreas(library.value || undefined)
    .then(data => { areas.value = data })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function openEdit(row: DataArea) {
  editForm.value = {
    library: row.DATA_AREA_LIBRARY,
    name: row.DATA_AREA_NAME,
    type: row.DATA_AREA_TYPE,
    value: row.DATA_AREA_VALUE,
  }
  editVisible.value = true
}

function openCreate() {
  createForm.value = { library: library.value || 'QSYS', name: '', length: 50, value: '' }
  createVisible.value = true
}

async function handleSave() {
  if (!editForm.value) return
  saving.value = true
  try {
    await updateDataArea(editForm.value.library, editForm.value.name, editForm.value.value)
    ElMessage.success(t('common.updateSuccess'))
    editVisible.value = false
    load()
  } catch {
    /* interceptor handles error */
  } finally {
    saving.value = false
  }
}

async function handleCreate() {
  if (!createForm.value.name) return
  saving.value = true
  try {
    await createDataArea(createForm.value)
    ElMessage.success(t('common.addSuccess'))
    createVisible.value = false
    load()
  } catch {
    /* interceptor handles error */
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: DataArea) {
  try {
    await ElMessageBox.confirm(
      t('dataArea.deleteConfirm', { name: row.DATA_AREA_NAME }),
      t('common.confirm'),
      { type: 'warning' },
    )
  } catch {
    return
  }
  removeLoading.value = row.DATA_AREA_NAME
  try {
    await deleteDataArea(row.DATA_AREA_LIBRARY, row.DATA_AREA_NAME)
    ElMessage.success(t('common.deleteSuccess'))
    load()
  } catch {
    /* interceptor handles error */
  } finally {
    removeLoading.value = null
  }
}
</script>
