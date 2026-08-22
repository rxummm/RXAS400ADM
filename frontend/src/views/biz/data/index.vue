<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="$t('bizData.keyword')"
      :from-cache="isFromCache"
      :flash-tick="dataSourceTick"
      @force-search="handleRefresh"
      @reset="resetSearch"
    >
      <el-input
        v-model="library"
        :placeholder="$t('bizData.library')"
        clearable
        class="w-160"
        @change="debouncedLoadTables"
        @keyup.enter="loadTables"
      />
      <el-select
        v-model="table"
        :placeholder="$t('bizData.selectTable')"
        filterable
        clearable
        class="w-220"
        :loading="tableLoading"
        @change="onTableChange"
      >
        <el-option
          v-for="tb in tableOptions"
          :key="tb.TABLE_NAME"
          :label="tb.TABLE_TEXT ? $t('bizData.tableLabel', { name: tb.TABLE_NAME, text: tb.TABLE_TEXT }) : tb.TABLE_NAME"
          :value="tb.TABLE_NAME"
        />
      </el-select>
      <template #right>
        <span class="hint">{{ $t('bizData.hint') }}</span>
      </template>
    </QueryBar>

    <div class="table-wrapper">
      <el-alert v-if="table" type="info" :closable="false" class="mb8">
        <template #title>
          {{ $t('bizData.currentTable') }}: <b>{{ library }}.{{ table }}</b>
          <span v-if="fieldCount">（{{ $t('bizData.fieldCount', { n: fieldCount }) }}）</span>
          <span v-if="isFromCache" class="ml8">· {{ $t('common.fromCache') }}</span>
        </template>
      </el-alert>

      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="rows" size="small" border :max-height="tableMaxHeight">
        <template v-if="columns.length">
          <el-table-column
            v-for="col in columns"
            :key="col.COLUMN_NAME"
            :prop="col.COLUMN_NAME"
            :label="col.COLUMN_NAME"
            min-width="110"
            show-overflow-tooltip
          />
        </template>
        <el-table-column v-else :label="$t('bizData.noData')" />
      </el-table>
      </RxSkeleton>
      <AppPagination
        :total="total"
        v-model:current="page"
        v-model:size="size"
        @change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BizData' })
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { bizData, listBizTables, type BizColumn, type BizTable } from '@/api/business'
import AppPagination from '@/components/AppPagination.vue'
import QueryBar from '@/components/QueryBar.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import { debounce } from '@/utils/debounce'

const { t } = useI18n()

const library = ref('APP')
const table = ref<string | null>(null)
const tableOptions = ref<BizTable[]>([])
const tableLoading = ref(false)
const columns = ref<BizColumn[]>([])
const fieldCount = ref(0)

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

// 服务端分页 + 3 分钟查询缓存 + 已加载数据前端实时模糊匹配（动态列任意字段命中）
const {
  records,
  pagedData: rows,
  loading,
  keyword,
  current: page,
  size,
  total,
  isFromCache,
  dataSourceTick,
  resetSearch: baseResetSearch,
  fetchData,
  handlePageChange,
  handleSizeChange,
  tableMaxHeight,
} = useSmartQueryTable<Record<string, unknown>>({
  fetchApi: async (params) => {
    const result = await bizData(
      params as { library: string; table: string; keyword?: string; page?: number; size?: number },
    )
    columns.value = result.columns || []
    fieldCount.value = columns.value.length
    return { records: result.rows || [], total: result.total || 0 }
  },
  frontendPage: false,
  enableCache: true,
  autoFetch: false,
  buildParams: (base) => ({
    library: library.value.trim(),
    table: table.value,
    keyword: base.keyword || undefined,
    page: base.current,
    size: base.size,
  }),
  // 动态列：任意字段命中即算匹配（输入框实时过滤已加载数据，不发请求）
  matchRow: (row, kw) =>
    Object.values(row).some((v) => v != null && String(v).toLowerCase().includes(kw)),
})

const load = () => {
  if (!library.value.trim() || !table.value) {
    ElMessage.warning(t('bizData.selectFirst'))
    return
  }
  void fetchData()
}

const onTableChange = async () => {
  if (!table.value) {
    columns.value = []
    records.value = []
    total.value = 0
    return
  }
  page.value = 1
  await load()
}

const handleRefresh = () => {
  loadTables()
  load()
}

const resetSearch = () => {
  baseResetSearch()
  loadTables()
  load()
}

onMounted(loadTables)
</script>

<style scoped>
/* mb8/hint 已收敛至 src/styles/common.css */
</style>