<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" class="w-120" :placeholder="$t('bpcs.label.cono')" clearable />
      <el-date-picker v-model="fromDate" type="date" value-format="YYYYMMDD" class="w-200"
        :placeholder="$t('bpcs.abcXyz.fromDate')" />
      <el-button type="primary" :loading="loading" @click="load">
        {{ $t('common.search') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <!-- 9 宫格矩阵 -->
      <el-card shadow="never" class="mb16">
        <template #header>
          <span class="font-bold">{{ $t('bpcs.abcXyz.matrixTitle') }}</span>
        </template>
        <el-table :data="matrixData" v-loading="loading" size="small" border>
          <el-table-column prop="item" :label="$t('bpcs.label.item')" min-width="120" />
          <el-table-column prop="itemDesc" :label="$t('bpcs.label.itemDesc')" min-width="150" show-overflow-tooltip />
          <el-table-column prop="totalQty" :label="$t('bpcs.abcXyz.totalQty')" width="100" align="right" />
          <el-table-column prop="stockValue" :label="$t('bpcs.abcXyz.stockValue')" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.stockValue) }}</template>
          </el-table-column>
          <el-table-column prop="abcClass" :label="$t('bpcs.abcXyz.abcClass')" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.abcClass === 'A' ? 'danger' : row.abcClass === 'B' ? 'warning' : 'info'" size="small" effect="dark">
                {{ row.abcClass }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="cv" :label="$t('bpcs.abcXyz.cv')" width="80" align="right">
            <template #default="{ row }">{{ row.cv != null ? row.cv.toFixed(2) : '—' }}</template>
          </el-table-column>
          <el-table-column prop="xyzClass" :label="$t('bpcs.abcXyz.xyzClass')" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.xyzClass === 'X' ? 'success' : row.xyzClass === 'Y' ? 'warning' : 'info'" size="small" effect="dark">
                {{ row.xyzClass }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="matrixCell" :label="$t('bpcs.abcXyz.matrixCell')" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="matrixTagType(row.matrixCell)" size="small" effect="plain">
                {{ row.matrixCell }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsAbcXyz' })

import { ref } from 'vue'
import { getAbcXyzMatrix, type AbcXyzItem } from '@/api/bpcs'

const cono = ref('001')
const fromDate = ref('20250101')
const loading = ref(false)
const matrixData = ref<AbcXyzItem[]>([])

async function load() {
  loading.value = true
  try {
    matrixData.value = await getAbcXyzMatrix({ cono: cono.value, fromDate: fromDate.value, limit: 100 })
  } catch {
    /* interceptor handles error */
  } finally {
    loading.value = false
  }
}

function formatMoney(val: number | null | undefined) {
  if (val == null) return '—'
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'USD' }).format(val)
}

function matrixTagType(cell: string) {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    AX: 'danger', AY: 'danger', AZ: 'danger',
    BX: 'warning', BY: 'warning', BZ: 'info',
    CX: 'info', CY: 'info', CZ: 'info'
  }
  return map[cell] || 'info'
}

load()
</script>
