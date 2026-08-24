<template>
  <div class="page-container page-container--fit">
    <div class="dict-layout">
      <!-- 左：字典类型 -->
      <div class="dict-types">
        <div class="search-bar">
          <el-input v-model="typeKeyword" :placeholder="$t('dict.searchType')" clearable class="w-160" />
          <el-button type="primary" :icon="Plus" v-has-perm="'DICT_MANAGE'" @click="openTypeDialog()">
            {{ $t('dict.addType') }}
          </el-button>
        </div>
        <div class="table-wrapper">
          <RxSkeleton type="table" :rows="5" :loading="typesLoading">
          <el-table
            :data="filteredTypes"
            size="small"
            border
            highlight-current-row
            @row-click="selectType"
          >
            <el-table-column prop="code" :label="$t('dict.code')" min-width="110" />
            <el-table-column prop="name" :label="$t('dict.name')" min-width="100" />
            <el-table-column :label="$t('common.status')" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? $t('common.enable') : $t('common.disable') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="$t('common.operation')" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click.stop="openTypeDialog(row as DictType)">{{ $t('common.edit') }}</el-button>
                <el-button link type="danger" size="small" @click.stop="removeType(row as DictType)">{{ $t('common.delete') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          </RxSkeleton>
          <el-empty v-if="!typesLoading && filteredTypes.length === 0" :description="$t('dict.emptyTypes')" :image-size="60" />
        </div>
      </div>

      <!-- 右：字典项（子组件） -->
      <DictItemList ref="dictItemListRef" :current-type="currentType" />
    </div>

    <!-- 类型弹窗 -->
    <el-dialog v-model="typeDialog" :title="typeForm.id ? $t('common.edit') : $t('dict.addType')" width="460px">
      <el-form :model="typeForm" label-width="80px">
        <el-form-item :label="$t('dict.code')" required>
          <el-input v-model="typeForm.code" :disabled="!!typeForm.id" placeholder="job_status" />
        </el-form-item>
        <el-form-item :label="$t('dict.name')" required>
          <el-input v-model="typeForm.name" />
        </el-form-item>
        <el-form-item :label="$t('dict.remark')">
          <el-input v-model="typeForm.remark" />
        </el-form-item>
        <el-form-item :label="$t('common.sort')">
          <el-input-number v-model="typeForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-switch v-model="typeForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveType">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import {
  createDictType,
  deleteDictType,
  listDictTypes,
  updateDictType,
  type DictType,
} from '@/api/dict'
import RxSkeleton from '@/components/RxSkeleton.vue'
import DictItemList from './DictItemList.vue'

defineOptions({ name: 'DictManage' })

const { t } = useI18n()

const types = ref<DictType[]>([])
const typesLoading = ref(false)
const typeKeyword = ref('')
const currentType = ref<DictType | null>(null)
const saving = ref(false)
const dictItemListRef = ref<InstanceType<typeof DictItemList>>()

const filteredTypes = computed(() => {
  const kw = typeKeyword.value.trim().toLowerCase()
  if (!kw) return types.value
  return types.value.filter((ty) => (ty.name || '').toLowerCase().includes(kw) || (ty.code || '').toLowerCase().includes(kw))
})

const loadTypes = async () => {
  typesLoading.value = true
  try {
    types.value = (await listDictTypes()) || []
    // 默认选中第一项
    if (!currentType.value && types.value.length) {
      await selectType(types.value[0])
    }
  } finally {
    typesLoading.value = false
  }
}

const selectType = (row: DictType) => {
  currentType.value = row
}

// ==================== 类型 CRUD ====================
const typeDialog = ref(false)
const typeForm = ref<Partial<DictType>>({ code: '', name: '', remark: '', sort: 0, status: 1 })

const openTypeDialog = (row?: DictType) => {
  typeForm.value = row ? { ...row } : { code: '', name: '', remark: '', sort: 0, status: 1 }
  typeDialog.value = true
}

const saveType = async () => {
  if (!typeForm.value.code || !typeForm.value.name) {
    ElMessage.warning(t('dict.required'))
    return
  }
  saving.value = true
  try {
    if (typeForm.value.id) {
      await updateDictType(typeForm.value.id, typeForm.value as DictType)
    } else {
      await createDictType(typeForm.value as DictType)
    }
    ElMessage.success(t('common.save'))
    typeDialog.value = false
    await loadTypes()
  } finally {
    saving.value = false
  }
}

const removeType = async (row: DictType) => {
  try {
    await ElMessageBox.confirm(t('dict.deleteTypeConfirm', { name: row.name }), t('common.tip'), { type: 'warning' })
    if (row.id) await deleteDictType(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    if (currentType.value?.id === row.id) {
      currentType.value = null
    }
    await loadTypes()
  } catch {
    /* cancelled */
  }
}



onMounted(loadTypes)
</script>

<style scoped>
.dict-layout {
  display: flex;
  gap: 12px;
  height: 100%;
  min-height: 0;
}
.dict-types {
  flex: 0 0 42%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

</style>