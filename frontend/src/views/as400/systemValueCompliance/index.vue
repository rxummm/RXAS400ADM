<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-select v-model="serverId" clearable :placeholder="$t('systemValueCompliance.serverId')" class="mr4">
        <el-option v-for="s in servers" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="load">
        {{ $t('common.search') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <el-table :data="records" v-loading="loading" size="small" border>
        <el-table-column prop="serverName" :label="$t('systemValueCompliance.serverId')" min-width="120" />
        <el-table-column prop="systemValue" :label="$t('systemValueCompliance.systemValue')" min-width="140" />
        <el-table-column prop="currentValue" :label="$t('systemValueCompliance.currentValue')" min-width="120" />
        <el-table-column prop="expectedValue" :label="$t('systemValueCompliance.expectedValue')" min-width="120" />
        <el-table-column prop="complianceStatus" :label="$t('systemValueCompliance.complianceStatus')" width="110">
          <template #default="{ row }">
            <el-tag :type="complianceType(row.complianceStatus)" size="small">
              {{ $t('systemValueCompliance.' + row.complianceStatus.toLowerCase()) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="severity" :label="$t('systemValueCompliance.severity')" width="100">
          <template #default="{ row }">
            <el-tag :type="severityType(row.severity)" size="small">
              {{ $t('systemValueCompliance.' + row.severity.toLowerCase()) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('systemValueCompliance.description')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="remediation" :label="$t('systemValueCompliance.remediation')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="lastChecked" :label="$t('systemValueCompliance.lastChecked')" width="170" />
      </el-table>
      <el-empty v-if="!loading && records.length === 0" :description="$t('systemValueCompliance.noData')" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'SystemValueCompliance' })

import { ref, onMounted } from 'vue'
import { getComplianceList, type SystemValueCompliance } from '@/api/bpcs'
import { fetchSystems, type IbmiSystem } from '@/api/as400'

const loading = ref(false)
const serverId = ref<number | undefined>(undefined)
const records = ref<SystemValueCompliance[]>([])
const servers = ref<IbmiSystem[]>([])

function complianceType(status: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    PASS: 'success',
    FAIL: 'danger',
    WARNING: 'warning',
  }
  return map[status] || 'info'
}

function severityType(severity: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    CRITICAL: 'danger',
    HIGH: 'warning',
    MEDIUM: 'primary',
    LOW: 'info',
  }
  return map[severity] || 'info'
}

async function loadServers() {
  try {
    servers.value = await fetchSystems()
  } catch {
    servers.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const res = await getComplianceList(serverId.value != null ? { serverId: serverId.value } : undefined)
    records.value = res
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadServers()
  load()
})
</script>
