<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-select v-model="serverId" clearable :placeholder="$t('backupMonitor.serverId')" class="mr4">
        <el-option v-for="s in servers" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="load">
        {{ $t('common.search') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <el-table :data="records" v-loading="loading" size="small" border>
        <el-table-column prop="serverName" :label="$t('backupMonitor.serverId')" min-width="120" />
        <el-table-column prop="backupName" :label="$t('backupMonitor.backupName')" min-width="140" />
        <el-table-column prop="backupType" :label="$t('backupMonitor.backupType')" width="110">
          <template #default="{ row }">
            {{ $t('backupMonitor.' + row.backupType.toLowerCase()) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="$t('backupMonitor.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ $t('backupMonitor.' + row.status.toLowerCase()) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" :label="$t('backupMonitor.startTime')" width="170" />
        <el-table-column prop="endTime" :label="$t('backupMonitor.endTime')" width="170" />
        <el-table-column prop="durationSeconds" :label="$t('backupMonitor.duration')" width="100">
          <template #default="{ row }">
            {{ row.durationSeconds != null ? row.durationSeconds + 's' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="objectsCount" :label="$t('backupMonitor.objectsCount')" width="100" />
        <el-table-column prop="sizeBytes" :label="$t('backupMonitor.sizeBytes')" width="100">
          <template #default="{ row }">
            {{ row.sizeBytes != null ? formatSize(row.sizeBytes) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="mediaName" :label="$t('backupMonitor.mediaName')" min-width="100" />
        <el-table-column prop="errorMessage" :label="$t('backupMonitor.errorMessage')" min-width="160" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!loading && records.length === 0" :description="$t('backupMonitor.noData')" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BackupMonitor' })

import { ref, onMounted } from 'vue'
import { getBackupList, type BackupStatus } from '@/api/bpcs'
import { fetchSystems, type IbmiSystem } from '@/api/as400'

const loading = ref(false)
const serverId = ref<number | undefined>(undefined)
const records = ref<BackupStatus[]>([])
const servers = ref<IbmiSystem[]>([])

function statusType(status: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    SUCCESS: 'success',
    FAILED: 'danger',
    RUNNING: 'primary',
    WARNING: 'warning',
  }
  return map[status] || 'info'
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1073741824) return (bytes / 1048576).toFixed(1) + ' MB'
  return (bytes / 1073741824).toFixed(1) + ' GB'
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
    const res = await getBackupList(serverId.value != null ? { serverId: serverId.value } : undefined)
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
