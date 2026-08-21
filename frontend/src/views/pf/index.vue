<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="library" :placeholder="$t('pf.library')" clearable class="w-180" @keyup.enter="loadFiles" />
      <el-button type="primary" @click="loadFiles">{{ $t('common.search') }}</el-button>
      <el-select v-model="file" :placeholder="$t('pf.selectFile')" clearable class="w-220" @change="openFile">
        <el-option v-for="f in files" :key="f.TABLE_NAME" :label="`${f.TABLE_NAME} (${f.TABLE_TEXT || ''})`" :value="f.TABLE_NAME" />
      </el-select>
    </div>

    <el-card shadow="never" class="pf-card">
      <template #header>
        <span>{{ $t('pf.fileDetail', { file: file || '-' }) }}</span>
      </template>
      <template v-if="file">
        <h4 class="section">{{ $t('pf.columns') }}</h4>
        <RxSkeleton type="table" :rows="5" :loading="colsLoading">
          <el-table :data="columns" size="small" border>
          <el-table-column prop="COLUMN_NAME" :label="$t('pf.colName')" min-width="130" />
          <el-table-column prop="COLUMN_TYPE" :label="$t('pf.colType')" width="120" />
          <el-table-column prop="LENGTH" :label="$t('pf.colLen')" width="90" />
          <el-table-column prop="NULLABLE" :label="$t('pf.nullable')" width="90" />
        </el-table>
        </RxSkeleton>
        <h4 class="section">{{ $t('pf.data') }}</h4>
        <QueryBar
          v-model:keyword="keyword"
          :placeholder="$t('common.keyword')"
          :from-cache="isFromCache"
          :flash-tick="dataSourceTick"
          :keyword-width="180"
          @force-search="handleRefresh"
          @reset="resetSearch"
        >
          <el-input-number v-model="limit" :min="1" :max="200" />
        </QueryBar>
        <div class="table-wrapper">
          <RxSkeleton type="table" :rows="8" :loading="dataLoading">
            <el-table :data="pagedData" size="small" border>
            <el-table-column
              v-for="col in columns"
              :key="col.COLUMN_NAME"
              :prop="col.COLUMN_NAME"
              :label="col.COLUMN_NAME"
              min-width="110"
            />
          </el-table>
          </RxSkeleton>
          <el-empty v-if="!dataLoading && total === 0" :description="$t('pf.empty')" />
          <AppPagination :total="total" v-model:current="dataCurrent" v-model:size="dataSize" @change="handlePageChange" @size-change="handleSizeChange" />
        </div>
      </template>
      <el-empty v-else :description="$t('pf.selectFirst')" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import QueryBar from '@/components/QueryBar.vue'
import { pfColumns, pfData, pfFiles, type PfColumn, type PfFile } from '@/api/pf'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'

const library = ref('APP')
const files = ref<PfFile[]>([])
const file = ref<string | null>(null)
const columns = ref<PfColumn[]>([])
const limit = ref(20)
const colsLoading = ref(false)

const {
  pagedData,
  loading: dataLoading,
  keyword,
  current: dataCurrent,
  size: dataSize,
  total,
  isFromCache,
  dataSourceTick,
  resetSearch,
  handleRefresh,
  handlePageChange,
  handleSizeChange,
  fetchData,
} = useSmartQueryTable<Record<string, unknown>>({
  fetchApi: (params) =>
    pfData(params.library as string, params.file as string, params.limit as number),
  frontendPage: true,
  enableCache: true,
  autoFetch: false,
  buildParams: () => ({ library: library.value, file: file.value, limit: limit.value }),
  // 动态列：关键字匹配任意列值
  matchRow: (row, kw) =>
    Object.values(row).some((v) => v != null && String(v).toLowerCase().includes(kw)),
})

const loadData = () => {
  if (!file.value) return
  void fetchData()
}

const loadFiles = async () => {
  try {
    files.value = await pfFiles(library.value)
  } catch {
    files.value = []
  }
}

const openFile = async (name: string) => {
  if (!name) return
  file.value = name
  colsLoading.value = true
  try {
    columns.value = await pfColumns(library.value, name)
  } catch {
    columns.value = []
  } finally {
    colsLoading.value = false
  }
  loadData()
}

onMounted(loadFiles)
</script>

<style scoped>
.pf-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.pf-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
</style>