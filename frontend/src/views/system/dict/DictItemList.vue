<template>
  <div class="dict-items">
    <div class="items-header">
      <span class="items-title">
        {{ $t('dict.items') }}<span v-if="currentType" class="items-code">- {{ currentType.name }} ({{ currentType.code }})</span>
      </span>
      <el-button
        type="primary"
        size="small"
        :icon="Plus"
        v-has-perm="'DICT_MANAGE'"
        :disabled="!currentType"
        @click="openItemDialog()"
      >
        {{ $t('dict.addItem') }}
      </el-button>
    </div>
    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="items" size="small" border>
        <el-table-column prop="itemKey" :label="$t('dict.itemKey')" min-width="120" />
        <el-table-column prop="itemValue" :label="$t('dict.itemValue')" min-width="140" />
        <el-table-column prop="sort" :label="$t('common.sort')" width="70" align="center" />
        <el-table-column :label="$t('common.status')" width="90" align="center">
          <template #default="{ row }: { row: DictItem }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? $t('common.enable') : $t('common.disable') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="130" fixed="right">
          <template #default="{ row }: { row: DictItem }">
            <el-button link type="primary" size="small" @click="openItemDialog(row)">{{ $t('common.edit') }}</el-button>
            <el-button link type="danger" size="small" @click="removeItem(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <el-empty v-if="!loading && items.length === 0" :description="$t('dict.emptyItems')" :image-size="60" />
    </div>

    <el-dialog v-model="itemDialog" :title="itemForm.id ? $t('common.edit') : $t('dict.addItem')" width="460px">
      <el-form :model="itemForm" label-width="80px">
        <el-form-item :label="$t('dict.itemKey')" required>
          <el-input v-model="itemForm.itemKey" :disabled="!!itemForm.id" />
        </el-form-item>
        <el-form-item :label="$t('dict.itemValue')" required>
          <el-input v-model="itemForm.itemValue" />
        </el-form-item>
        <el-form-item :label="$t('common.sort')">
          <el-input-number v-model="itemForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-switch v-model="itemForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialog = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveItem">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import {
  createDictItem,
  deleteDictItem,
  listDictItems,
  updateDictItem,
  type DictItem,
  type DictType,
} from '@/api/dict'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t } = useI18n()

const props = defineProps<{
  currentType: DictType | null
}>()



const items = ref<DictItem[]>([])
const loading = ref(false)
const saving = ref(false)

const loadItems = async (typeCode: string) => {
  loading.value = true
  try {
    items.value = (await listDictItems(typeCode)) || []
  } finally {
    loading.value = false
  }
}

watch(
  () => props.currentType,
  (ty) => {
    if (ty) loadItems(ty.code)
    else items.value = []
  },
)

// ==================== 字典项 CRUD ====================
const itemDialog = ref(false)
const itemForm = ref<Partial<DictItem>>({ typeCode: '', itemKey: '', itemValue: '', sort: 0, status: 1 })

const openItemDialog = (row?: DictItem) => {
  itemForm.value = row ? { ...row } : { typeCode: props.currentType?.code, itemKey: '', itemValue: '', sort: 0, status: 1 }
  itemDialog.value = true
}

const saveItem = async () => {
  if (!itemForm.value.itemKey || !itemForm.value.itemValue) {
    ElMessage.warning(t('dict.required'))
    return
  }
  saving.value = true
  try {
    if (itemForm.value.id) {
      await updateDictItem(itemForm.value.id, itemForm.value as DictItem)
    } else {
      await createDictItem(itemForm.value as DictItem)
    }
    ElMessage.success(t('common.save'))
    itemDialog.value = false
    if (props.currentType) await loadItems(props.currentType.code)
  } finally {
    saving.value = false
  }
}

const removeItem = async (row: DictItem) => {
  try {
    await ElMessageBox.confirm(t('dict.deleteItemConfirm', { name: row.itemKey }), t('common.tip'), { type: 'warning' })
    if (row.id) await deleteDictItem(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    if (props.currentType) await loadItems(props.currentType.code)
  } catch {
    /* cancelled */
  }
}

defineExpose({ loadItems })
</script>

<style scoped>
.dict-items {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 2px;
}
.items-title {
  font-weight: 600;
}
.items-code {
  color: var(--text-secondary);
  font-weight: 400;
  font-size: 13px;
  margin-left: 6px;
}
.table-wrapper {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
</style>
