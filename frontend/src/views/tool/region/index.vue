<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        :placeholder="$t('tool.region.searchName')"
        clearable
        class="w-200"
        @keyup.enter="loadRoot"
      />
      <el-select v-model="level" :placeholder="$t('tool.region.filterByLevel')" clearable class="w-130">
        <el-option :label="$t('tool.region.levelOptions.province')" :value="1" />
        <el-option :label="$t('tool.region.levelOptions.city')" :value="2" />
        <el-option :label="$t('tool.region.levelOptions.district')" :value="3" />
      </el-select>
      <el-button type="primary" @click="loadRoot">
        <el-icon><Search /></el-icon> {{ $t('common.search') }}
      </el-button>
      <el-button @click="resetSearch">{{ $t('common.reset') }}</el-button>
      <div class="flex-1" />
      <el-button type="primary" @click="openCreate(null)">
        <el-icon><Plus /></el-icon> {{ $t('tool.region.addRegion') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table
          :data="pagedRoots"
          row-key="id"
          border
          stripe
          size="small"
          lazy
        :load="loadChildren"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        class="w-full"
      >
        <el-table-column prop="name" :label="$t('tool.region.name')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="code" :label="$t('tool.region.code')" width="130" />
        <el-table-column :label="$t('tool.region.level')" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="levelTag(row.level)">{{ levelLabel(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pinyin" :label="$t('tool.region.pinyin')" width="120" show-overflow-tooltip />
        <el-table-column prop="abbreviation" :label="$t('tool.region.abbreviation')" width="90" />
        <el-table-column prop="status" :label="$t('common.status')" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? $t('common.yes') : $t('common.no') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.level < 3" link type="primary" size="small" @click="openCreate(row as Region)">
              {{ $t('tool.region.addChild') }}
            </el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as Region)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button link type="danger" size="small" @click="onDelete(row as Region)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="rootRegions.length" v-model:current="current" v-model:size="size" @change="() => {}" @size-change="onSizeChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item :label="$t('tool.region.code')" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" maxlength="12" />
        </el-form-item>
        <el-form-item :label="$t('tool.region.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="$t('tool.region.level')">
          <el-radio-group v-model="form.level" :disabled="isEdit">
            <el-radio :value="1">{{ $t('tool.region.levelOptions.province') }}</el-radio>
            <el-radio :value="2">{{ $t('tool.region.levelOptions.city') }}</el-radio>
            <el-radio :value="3">{{ $t('tool.region.levelOptions.district') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('tool.region.parent')">
          <el-input :model-value="parentName || '-'" disabled />
        </el-form-item>
        <el-form-item :label="$t('tool.region.pinyin')">
          <el-input v-model="form.pinyin" />
        </el-form-item>
        <el-form-item :label="$t('tool.region.abbreviation')">
          <el-input v-model="form.abbreviation" maxlength="16" />
        </el-form-item>
        <el-form-item :label="$t('common.sort')">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchRegionChildren, createRegion, updateRegion, deleteRegion, type Region } from '@/api/region'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'

defineOptions({ name: 'RegionManage' })

const { t } = useI18n()

const loading = ref(false)
const rootRegions = ref<Region[]>([])
const keyword = ref('')
const level = ref<number>()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const parentName = ref('')
const current = ref(1)
const size = ref(15)

const pagedRoots = computed(() =>
  rootRegions.value.slice((current.value - 1) * size.value, current.value * size.value),
)

const onSizeChange = () => {
  current.value = 1
}

const levelLabel = (l: number) =>
  l === 1 ? t('tool.region.levelOptions.province') : l === 2 ? t('tool.region.levelOptions.city') : t('tool.region.levelOptions.district')
const levelTag = (l: number) => (l === 1 ? 'danger' : l === 2 ? 'warning' : 'info')

const defaultForm = () => ({
  id: undefined as number | undefined,
  code: '',
  name: '',
  level: 1,
  parentCode: '',
  pinyin: '',
  abbreviation: '',
  sort: 0,
  status: 1,
})
const form = reactive(defaultForm())

const formRules = {
  code: [{ required: true, message: () => t('tool.region.codeRequired'), trigger: 'blur' }],
  name: [{ required: true, message: () => t('tool.region.nameRequired'), trigger: 'blur' }],
}

async function loadRoot() {
  loading.value = true
  current.value = 1
  try {
    rootRegions.value = withHasChildren(await fetchRegionChildren())
    if (keyword.value || level.value) {
      rootRegions.value = rootRegions.value.filter(
        (r) =>
          (!keyword.value || r.name?.includes(keyword.value) || r.code?.includes(keyword.value)) &&
          (!level.value || r.level === level.value),
      )
    }
  } finally {
    loading.value = false
  }
}

/** el-table 懒加载需要 hasChildren 标志（省/市可展开，区县为叶子） */
const withHasChildren = (list: Region[]): Region[] =>
  list.map((r) => ({ ...r, hasChildren: r.level < 3 }))

const loadChildren = async (row: Region, _treeNode: unknown, resolve: (data: Region[]) => void) => {
  try {
    const children = await fetchRegionChildren(row.code)
    resolve(withHasChildren(children as Region[]))
  } catch {
    resolve([])
  }
}

const resetSearch = () => {
  keyword.value = ''
  level.value = undefined
  loadRoot()
}

function openCreate(parent: Region | null) {
  isEdit.value = false
  dialogTitle.value = t('tool.region.addRegion')
  Object.assign(form, defaultForm())
  form.parentCode = parent?.code || ''
  parentName.value = parent?.name || ''
  form.level = parent ? parent.level + 1 : 1
  dialogVisible.value = true
}

function openEdit(row: Region) {
  isEdit.value = true
  dialogTitle.value = t('common.edit')
  Object.assign(form, defaultForm(), {
    id: row.id,
    code: row.code,
    name: row.name,
    level: row.level,
    parentCode: row.parentCode || '',
    pinyin: row.pinyin || '',
    abbreviation: row.abbreviation || '',
    sort: row.sort ?? 0,
    status: row.status ?? 1,
  })
  parentName.value = ''
  dialogVisible.value = true
}

async function onDelete(row: Region) {
  try {
    await ElMessageBox.confirm(
      t('tool.region.deleteConfirm', { name: row.name }),
      t('common.tip'),
      { type: 'warning' },
    )
    if (row.id) await deleteRegion(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    loadRoot()
  } catch {
    /* cancelled */
  }
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value && form.id) {
      await updateRegion(form.id, { ...form })
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createRegion({ ...form })
      ElMessage.success(t('common.addSuccess'))
    }
    dialogVisible.value = false
    loadRoot()
  } finally {
    submitLoading.value = false
  }
}

onMounted(loadRoot)
</script>