<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <span v-if="report" class="gen">{{ $t('health.generatedAt') }}: {{ report.generatedAt }}</span>
      <div class="flex-1" />
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="load">
        {{ $t('common.refresh') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <div class="summary">
        <div class="health-item">
          <span class="h-label">{{ $t('health.database') }}</span>
          <el-tag size="small" :type="!report ? 'info' : report.database === 'OK' ? 'success' : 'danger'">
            {{ report?.database || '-' }}
          </el-tag>
        </div>
        <div class="health-item">
          <span class="h-label">{{ $t('health.scheduler') }}</span>
          <el-tag size="small" :type="!report ? 'info' : report.scheduler === 'RUNNING' ? 'success' : 'danger'">
            {{ report?.scheduler || '-' }}
          </el-tag>
        </div>
        <el-statistic :title="$t('health.openAlerts')" :value="report?.openAlerts ?? 0" />
      </div>

      <div class="section">{{ $t('health.servers') }}</div>
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedServers" size="small" border>
        <el-table-column prop="name" :label="$t('health.name')" min-width="130" />
        <el-table-column prop="host" :label="$t('health.host')" min-width="150" />
        <el-table-column prop="environment" :label="$t('health.environment')" width="110" />
        <el-table-column :label="$t('health.connect')" width="120">
          <template #default="{ row }">
            <el-tag :type="row.connect === 'OK' ? 'success' : 'danger'" size="small">{{ row.connect }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" :label="$t('health.detail')" min-width="220" show-overflow-tooltip />
      </el-table>
      </RxSkeleton>
      <el-empty v-if="report && !loading && report.servers?.length === 0" :description="$t('health.noServers')" />
      <AppPagination :total="report?.servers?.length || 0" v-model:current="current" v-model:size="size" @change="() => {}" @size-change="onSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'Health' })
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { healthReport, type HealthReport, type HealthServer } from '@/api/health'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'

const report = ref<HealthReport | null>(null)
const loading = ref(false)
const current = ref(1)
const size = ref(10)

const servers = computed(() => report.value?.servers || [])

const pagedServers = computed(() =>
  servers.value.slice((current.value - 1) * size.value, current.value * size.value),
)

const onSizeChange = () => {
  current.value = 1
}

const load = async () => {
  loading.value = true
  try {
    report.value = await healthReport()
  } catch {
    report.value = null
    // health report failed silently (interceptor handles errors)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
/* mb16/mt16/section 已收敛至 src/styles/common.css */
.gen {
  color: var(--text-secondary);
  font-size: 13px;
}
.summary {
  display: flex;
  align-items: center;
  gap: 48px;
  padding: 4px 0 8px;
}
.health-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.h-label {
  font-size: 14px;
  color: var(--text-regular);
}
</style>