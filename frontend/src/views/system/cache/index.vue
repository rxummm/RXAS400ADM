<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        :placeholder="$t('cache.name')"
        clearable
        class="w-240"
        @keyup.enter="forceSearch"
      />
      <el-button type="primary" @click="onSearch">
        <el-icon><Search /></el-icon> {{ $t('common.search') }}
      </el-button>
      <el-button @click="resetSearch">{{ $t('common.reset') }}</el-button>
      <div class="flex-1" />
      <el-button type="danger" plain @click="onClearAll">
        <el-icon><Delete /></el-icon> {{ $t('cache.clearAll') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedRows" size="small" border stripe class="w-full">
        <el-table-column prop="name" :label="$t('cache.name')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="size" :label="$t('cache.size')" width="140" align="center">
          <template #default="{ row }">
            <span>{{ row.size ?? $t('cache.unknown') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onClear(row)">
              {{ $t('cache.clear') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="() => {}" @size-change="onSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Delete, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCaches, clearCache, clearAllCaches, type CacheInfo } from '@/api/cache'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'

defineOptions({ name: 'SysCache' })

const { t } = useI18n()

const {
  tableData,
  keyword,
  loading,
  current,
  size,
  total,
  forceSearch,
  resetSearch: baseResetSearch,
} = useSmartQueryTable<CacheInfo>({
  fetchApi: () => listCaches().then((data) => data || []),
  frontendPage: true,
  enableCache: true,
  searchFields: ['name'],
})

const pagedRows = computed(() => tableData.value)

const onSizeChange = () => {
  current.value = 1
}

async function onClear(row: CacheInfo) {
  try {
    await ElMessageBox.confirm(t('cache.clearConfirm', { name: row.name }), t('common.tip'), { type: 'warning' })
    await clearCache(row.name)
    ElMessage.success(t('cache.cleared'))
    forceSearch()
  } catch {
    /* cancelled */
  }
}

async function onClearAll() {
  try {
    await ElMessageBox.confirm(t('cache.clearAllConfirm'), t('common.tip'), { type: 'warning' })
    await clearAllCaches()
    ElMessage.success(t('cache.allCleared'))
    forceSearch()
  } catch {
    /* cancelled */
  }
}

const resetSearch = () => {
  baseResetSearch()
}

const onSearch = () => {
  forceSearch()
}
</script>