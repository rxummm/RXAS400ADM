<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="itemCode" :placeholder="$t('mrp.itemCode')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-select v-model="recommendType" :placeholder="$t('mrp.recommendType')" clearable class="search-bar__select">
        <el-option label="PURCHASE" value="PURCHASE" />
        <el-option label="PRODUCTION" value="PRODUCTION" />
        <el-option label="TRANSFER" value="TRANSFER" />
      </el-select>
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="recommendationNo" :label="$t('mrp.recommendationNo')" width="140" />
        <el-table-column prop="itemCode" :label="$t('mrp.itemCode')" width="120" />
        <el-table-column prop="itemDesc" :label="$t('mrp.itemDesc')" />
        <el-table-column prop="recommendQty" :label="$t('mrp.recommendQty')" width="120" />
        <el-table-column prop="recommendType" :label="$t('mrp.recommendType')" width="120" />
        <el-table-column prop="leadTimeDays" :label="$t('mrp.leadTimeDays')" width="100" />
        <el-table-column prop="suggestedDate" :label="$t('mrp.suggestedDate')" width="110" />
        <el-table-column prop="status" :label="$t('mrp.status')" width="100" />
        <el-table-column :label="$t('common.action')" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="primary" @click="release(row as MrpRecommendation)">
              {{ $t('mrp.release') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'MrpRecommendation' })

import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessageBox } from 'element-plus'
import { searchMrpRecommendations, releaseRecommendation, type MrpRecommendation } from '@/api/mrp'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'

const { t } = useI18n()

const itemCode = ref('')
const recommendType = ref('')

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<MrpRecommendation>({
  fetchApi: (params) => searchMrpRecommendations({
    itemCode: itemCode.value,
    recommendType: recommendType.value,
    current: params.current,
    size: params.size,
  }),
})

async function release(row: MrpRecommendation) {
  try {
    await ElMessageBox.confirm(
      t('mrp.releaseConfirm', { no: row.recommendationNo }),
      t('common.confirm'),
      { confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel'), type: 'warning' },
    )
    await releaseRecommendation(row.id)
    forceSearch()
  } catch {
    // User cancelled
  }
}

onMounted(() => forceSearch())
</script>