<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="library"
        :placeholder="$t('tableFields.library')"
        clearable
        class="w-160"
        @change="debouncedLoadTables"
        @keyup.enter="loadTables"
      />
      <el-select
        v-model="table"
        :placeholder="$t('tableFields.selectFile')"
        filterable
        clearable
        class="w-240"
        :loading="tableLoading"
        @change="onFileChange"
      >
        <el-option
          v-for="tb in tableOptions"
          :key="tb.TABLE_NAME"
          :label="tb.TABLE_TEXT ? $t('tableFields.tableLabel', { name: tb.TABLE_NAME, text: tb.TABLE_TEXT }) : tb.TABLE_NAME"
          :value="tb.TABLE_NAME"
        />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleSearch">{{ $t('common.search') }}</el-button>
      <el-button :icon="Refresh" @click="handleRefresh">{{ $t('common.refresh') }}</el-button>
      <span class="hint ml8">{{ $t('tableFields.hint') }}</span>
    </div>

    <div class="table-wrapper">
      <el-alert v-if="table" type="info" :closable="false" class="mb8">
        <template #title>
          {{ $t('tableFields.currentFile') }}: <b>{{ library }}.{{ table }}</b>
          <span v-if="columns.length">（{{ $t('tableFields.fieldCount', { n: columns.length }) }}）</span>
        </template>
      </el-alert>

      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="columns" size="small" border>
        <el-table-column prop="ORDINAL_POSITION" :label="$t('tableFields.ordinal')" width="70" align="center" />
        <el-table-column prop="COLUMN_NAME" :label="$t('tableFields.field')" min-width="140" />
        <el-table-column prop="DATA_TYPE" :label="$t('tableFields.type')" width="110" />
        <el-table-column :label="$t('tableFields.length')" width="90" align="right">
          <template #default="{ row }: { row: BizColumn }">
            <span v-if="row.LENGTH != null">{{ row.LENGTH }}<template v-if="row.SCALE != null && Number(row.SCALE) > 0">.{{ row.SCALE }}</template></span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('tableFields.nullable')" width="80" align="center">
          <template #default="{ row }: { row: BizColumn }">
            <el-tag :type="row.IS_NULLABLE === 'Y' ? 'warning' : 'info'" size="small">
              {{ row.IS_NULLABLE === 'Y' ? $t('common.yes') : $t('common.no') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="COLUMN_DEFAULT" :label="$t('tableFields.default')" width="120" show-overflow-tooltip>
          <template #default="{ row }: { row: BizColumn }">{{ row.COLUMN_DEFAULT ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="COLUMN_TEXT" :label="$t('tableFields.description')" min-width="220" show-overflow-tooltip>
          <template #default="{ row }: { row: BizColumn }">{{ row.COLUMN_TEXT || '-' }}</template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <el-empty v-if="!loading && table && !columns.length" :description="$t('tableFields.empty')" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { bizColumns, listBizTables, type BizColumn, type BizTable } from '@/api/business'
import { debounce } from '@/utils/debounce'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t } = useI18n()

const library = ref('APP')
const table = ref<string | null>(null)
const tableOptions = ref<BizTable[]>([])
const tableLoading = ref(false)
const columns = ref<BizColumn[]>([])
const loading = ref(false)

const loadTables = async () => {
  if (!library.value.trim()) {
    tableOptions.value = []
    return
  }
  tableLoading.value = true
  try {
    tableOptions.value = await listBizTables({ library: library.value.trim() })
  } catch {
    tableOptions.value = []
  } finally {
    tableLoading.value = false
  }
}

// P2-32：下拉 @change 直发查询防抖
const debouncedLoadTables = debounce(() => void loadTables())

const load = async () => {
  if (!library.value.trim() || !table.value) {
    ElMessage.warning(t('tableFields.selectFirst'))
    return
  }
  loading.value = true
  try {
    columns.value = await bizColumns(library.value.trim(), table.value)
  } finally {
    loading.value = false
  }
}

const onFileChange = () => {
  if (table.value) load()
  else columns.value = []
}

const handleSearch = () => {
  loadTables()
  load()
}

const handleRefresh = () => {
  loadTables()
  load()
}

onMounted(loadTables)
</script>

<style scoped>
/* mb8/hint 已收敛至 src/styles/common.css */
</style>