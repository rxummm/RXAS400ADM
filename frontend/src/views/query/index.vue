<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input
        v-model="sql"
        type="textarea"
        :rows="4"
        :placeholder="$t('query.placeholder')"
        class="sql-editor"
      />
      <div class="search-actions">
        <el-button v-has-perm="'QUERY_EXECUTE'" type="primary" :icon="Search" :loading="running" @click="run">
          {{ $t('query.execute') }}
        </el-button>
        <el-button @click="clear">{{ $t('query.clear') }}</el-button>
        <span v-if="result" class="meta">
          {{ $t('query.rows', { n: result.rowsReturned }) }} · {{ result.costMs }} ms
        </span>
        <div class="flex-1" />
      </div>
    </div>

    <div class="table-wrapper">
      <div class="section">{{ $t('query.result') }}</div>
      <el-table
        v-if="result"
        :data="pagedResult"
        size="small"
        border
        class="w-full"
      >
        <el-table-column
          v-for="col in result.columns"
          :key="col"
          :prop="col"
          :label="col"
          min-width="140"
          show-overflow-tooltip
        />
      </el-table>
      <el-empty v-else :description="$t('query.emptyResult')" :image-size="80" />
      <AppPagination v-if="result" :total="result.rows.length" v-model:current="resultCurrent" v-model:size="resultSize" @change="() => {}" @size-change="onResultSizeChange" />
    </div>

    <div class="table-wrapper mt16">
      <div class="section">
        <span>{{ $t('query.history') }}</span>
        <el-button :icon="Refresh" size="small" text @click="loadHistory">{{ $t('common.refresh') }}</el-button>
      </div>
      <el-table :data="pagedHistory" size="small" class="w-full">
        <el-table-column prop="sqlText" :label="$t('query.sql')" min-width="300" show-overflow-tooltip />
        <el-table-column prop="rowsReturned" :label="$t('query.rowsHeader')" width="100" />
        <el-table-column prop="costMs" :label="$t('query.cost')" width="100" />
        <el-table-column prop="operator" :label="$t('query.operator')" width="120" />
        <el-table-column prop="createdTime" :label="$t('query.time')" width="180" />
        <el-table-column :label="$t('common.operation')" width="90">
          <template #default="{ row }">
            <el-button size="small" @click="reuse(row.sqlText)">{{ $t('query.reuse') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination :total="history.length" v-model:current="historyCurrent" v-model:size="historySize" @change="() => {}" @size-change="onHistorySizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'Query' })
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { executeSql, fetchQueryHistory, type QueryHistoryRow, type QueryResult } from '@/api/query'
import AppPagination from '@/components/AppPagination.vue'

const { t } = useI18n()

const sql = ref('SELECT * FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) X')
const running = ref(false)
const result = ref<QueryResult | null>(null)
const history = ref<QueryHistoryRow[]>([])
const resultCurrent = ref(1)
const resultSize = ref(20)
const historyCurrent = ref(1)
const historySize = ref(10)

const pagedResult = computed(() =>
  result.value ? result.value.rows.slice((resultCurrent.value - 1) * resultSize.value, resultCurrent.value * resultSize.value) : [],
)
const pagedHistory = computed(() =>
  history.value.slice((historyCurrent.value - 1) * historySize.value, historyCurrent.value * historySize.value),
)

const onResultSizeChange = () => {
  resultCurrent.value = 1
}
const onHistorySizeChange = () => {
  historyCurrent.value = 1
}

const run = async () => {
  if (!sql.value.trim()) return
  try {
    await ElMessageBox.confirm(
      t('confirm.dangerExecuteCommand', { command: sql.value.substring(0, 50) + (sql.value.length > 50 ? '...' : '') }),
      t('confirm.dangerConfirm'),
      { type: 'warning' },
    )
  } catch {
    return
  }
  running.value = true
  resultCurrent.value = 1
  try {
    result.value = await executeSql(sql.value)
    await loadHistory()
  } catch {
    result.value = null
  } finally {
    running.value = false
  }
}

const clear = () => {
  sql.value = ''
  result.value = null
}

const loadHistory = async () => {
  try {
    history.value = await fetchQueryHistory(20)
    historyCurrent.value = 1
  } catch {
    history.value = []
  }
}

const reuse = (text: string) => {
  sql.value = text
}

onMounted(loadHistory)
</script>

<style scoped>
/* mb16/mt16/section 已收敛至 common.css */
.search-bar .sql-editor {
  width: 100%;
}
.search-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  width: 100%;
}
.sql-editor :deep(textarea) {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
}
.meta {
  margin-left: 12px;
  color: var(--text-secondary);
  font-size: 13px;
}
</style>