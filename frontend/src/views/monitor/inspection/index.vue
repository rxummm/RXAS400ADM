<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-select v-model="serverId" :placeholder="$t('inspection.selectServer')" class="w-200">
        <el-option
          v-for="s in servers"
          :key="s.id"
          :label="$t('inspection.serverLabel', { name: s.name, host: s.host })"
          :value="s.id"
        />
      </el-select>
      <el-button type="primary" :icon="Refresh" :loading="loading" :class="{ 'flash-pop': flashing }" @click="generate">
        {{ $t('inspection.generate') }}
      </el-button>
      <el-button type="success" :icon="Download" :disabled="!report" @click="exportPdf(false)">
        {{ $t('inspection.exportXlsx') }}
      </el-button>
      <el-button type="warning" :icon="Document" :disabled="!report" @click="exportPdf(true)">
        {{ $t('inspection.exportPdf') }}
      </el-button>
      <span v-if="report" class="hint ml8" :class="{ 'flash-pop': flashing }">
        {{ $t('inspection.generatedAt') }}: {{ report.generatedAt }}
      </span>
    </div>

    <template v-if="report">
      <div class="report-summary">
        <div class="score-card">
          <div class="score-value" :class="scoreClass">{{ report.score }}</div>
          <div class="score-grade">{{ report.grade }}</div>
          <div class="score-label">{{ $t('inspection.score') }}</div>
        </div>
        <div class="info-card">
          <div class="info-row"><span>{{ $t('inspection.server') }}</span><b>{{ report.serverName }}</b></div>
          <div class="info-row"><span>{{ $t('inspection.host') }}</span><b>{{ report.host }}</b></div>
          <div class="info-row"><span>{{ $t('inspection.environment') }}</span><b>{{ report.environment }}</b></div>
          <div class="info-row"><span>{{ $t('inspection.status') }}</span><b>{{ report.status }}</b></div>
        </div>
      </div>

      <div class="table-wrapper">
        <div class="section">{{ $t('inspection.checks') }}</div>
        <el-table :data="report.checks" size="small" border>
          <el-table-column :label="$t('inspection.checkName')" min-width="160">
            <template #default="{ row }">
              {{ $t('inspection.check.' + row.name) }}
            </template>
          </el-table-column>
          <el-table-column prop="value" :label="$t('inspection.checkValue')" min-width="180" />
          <el-table-column :label="$t('inspection.checkStatus')" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-wrapper mt16">
        <div class="section">{{ $t('inspection.issues') }} ({{ report.issues?.length ?? 0 }})</div>
        <el-table v-if="report.issues?.length" :data="report.issues" size="small" border>
          <el-table-column :label="$t('inspection.issueItem')" width="120">
            <template #default="{ row }">
              {{ $t('inspection.item.' + row.item) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('inspection.issueLevel')" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="row.level === 'CRITICAL' ? 'danger' : 'warning'" size="small">{{ row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('inspection.issueDetail')" min-width="260">
            <template #default="{ row }">
              {{ $t('inspection.detail.' + row.detail, row.detailParams || {}) }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else :description="$t('inspection.noIssue')" />
      </div>
    </template>
    <el-empty v-else-if="!loading" class="mt16" :description="$t('inspection.empty')" />
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'Inspection' })
import { computed, onMounted, ref } from 'vue'
import { Document, Download, Refresh } from '@element-plus/icons-vue'
import { useAs400ServerStore } from '@/stores/as400Server'
import { useFlash } from '@/composables/useFlash'
import { exportInspection, fetchInspection, type InspectionReport, type InspectionCheck, type InspectionIssue } from '@/api/monitor'

const as400Store = useAs400ServerStore()

const servers = computed(() => as400Store.serverList)
const serverId = ref<number>(0)
const loading = ref(false)
const report = ref<InspectionReport | null>(null)
/** 生成完成闪烁：报告取回后「生成」按钮与时间戳短暂高亮 */
const { flashing, trigger: flash } = useFlash()

const scoreClass = computed(() => {
  if (!report.value) return ''
  const s = Number(report.value.score)
  if (s >= 90) return 'score-ok'
  if (s >= 75) return 'score-warn'
  return 'score-bad'
})

const statusTag = (s: string) => (s === 'OK' ? 'success' : s === 'WARNING' ? 'warning' : 'danger')

const generate = async () => {
  if (!serverId.value) return
  loading.value = true
  try {
    report.value = await fetchInspection(serverId.value)
    flash()
  } finally {
    loading.value = false
  }
}

const exportPdf = async (pdf: boolean) => {
  if (!serverId.value) return
  await exportInspection(serverId.value, pdf ? 'pdf' : 'xlsx')
}

onMounted(async () => {
  if (!servers.value.length) {
    await as400Store.fetchServers()
  }
  if (servers.value.length) {
    serverId.value = as400Store.currentServerId || servers.value[0].id
  }
})
</script>

<style scoped>
.report-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}
.score-card {
  width: 160px;
  padding: 16px;
  text-align: center;
  background: var(--bg-container);
  border: 1px solid var(--border-color);
  border-radius: 8px;
}
.score-value {
  font-size: 40px;
  font-weight: 700;
  line-height: 1.2;
}
.score-ok {
  color: var(--color-success);
}
.score-warn {
  color: var(--color-warning);
}
.score-bad {
  color: var(--color-danger);
}
.score-grade {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
}
.score-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}
.info-card {
  flex: 1;
  padding: 16px;
  background: var(--bg-container);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 24px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}
.info-row span {
  color: var(--text-secondary);
}
.mt16 {
  margin-top: 16px;
}
</style>