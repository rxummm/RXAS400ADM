<template>
  <div class="page-container">
    <div class="search-bar">
      <el-button type="primary" :loading="loading" @click="load">
        {{ $t('common.search') }}
      </el-button>
    </div>

    <!-- A1: 服务器概览卡片 -->
    <el-row :gutter="16" class="mb16">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="section">{{ $t('systemHealth.totalServers') }}</div>
          <div class="count font-bold">{{ overview.totalServers }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="section">{{ $t('systemHealth.onlineServers') }}</div>
          <div class="count font-bold text-success">{{ overview.onlineServers }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="section">{{ $t('systemHealth.offlineServers') }}</div>
          <div class="count font-bold text-danger">{{ overview.offlineServers }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="section">{{ $t('systemHealth.activeJobs') }}</div>
          <div class="count font-bold">{{ overview.activeJobs }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mb16">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="section">{{ $t('systemHealth.avgCpu') }}</div>
          <div class="count font-bold">{{ overview.avgCpuUsage }}%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="section">{{ $t('systemHealth.avgMemory') }}</div>
          <div class="count font-bold">{{ overview.avgMemoryUsage }}%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="section">{{ $t('systemHealth.avgDisk') }}</div>
          <div class="count font-bold">{{ overview.avgDiskUsage }}%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="section">{{ $t('systemHealth.complianceRate') }}</div>
          <div class="count font-bold">{{ overview.complianceRate.toFixed(1) }}%</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mb16">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="section">{{ $t('systemHealth.alertCount') }}</div>
          <div class="count font-bold text-warning">{{ overview.alertCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="section">{{ $t('systemHealth.criticalAlerts') }}</div>
          <div class="count font-bold text-danger">{{ overview.criticalAlertCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="section">{{ $t('systemHealth.todayBackupSuccess') }}</div>
          <div class="count font-bold text-success">{{ overview.todayBackupSuccess }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="section">{{ $t('systemHealth.todayBackupFailed') }}</div>
          <div class="count font-bold text-danger">{{ overview.todayBackupFailed }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- A2: 安全审计摘要 -->
    <div class="section mb16">{{ $t('systemHealth.securityAudit') }}</div>
    <div class="table-wrapper mb16">
      <el-table :data="securityAudit" v-loading="loading" size="small" border>
        <el-table-column prop="serverName" :label="$t('systemHealth.serverOverview')" min-width="120" />
        <el-table-column prop="totalLogins" :label="$t('systemHealth.totalLogins')" width="100" />
        <el-table-column prop="successfulLogins" :label="$t('systemHealth.successfulLogins')" width="110" />
        <el-table-column prop="failedLogins" :label="$t('systemHealth.failedLogins')" width="100" />
        <el-table-column prop="uniqueUsers" :label="$t('systemHealth.uniqueUsers')" width="100" />
        <el-table-column prop="permissionChanges" :label="$t('systemHealth.permissionChanges')" width="110" />
        <el-table-column prop="highRiskOperations" :label="$t('systemHealth.highRiskOps')" width="100" />
      </el-table>
    </div>

    <!-- A3: 用户权限矩阵 -->
    <div class="section mb16">{{ $t('systemHealth.permissionMatrix') }}</div>
    <div class="table-wrapper">
      <el-table :data="permissionMatrix" v-loading="loading" size="small" border>
        <el-table-column prop="userName" :label="$t('users.username')" min-width="120" />
        <el-table-column prop="status" :label="$t('systemHealth.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('systemHealth.roles')" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" size="small" class="mr4">{{ role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeCount" :label="$t('systemHealth.changeCount')" width="100" />
        <el-table-column prop="passwordExpireDays" :label="$t('systemHealth.passwordExpireDays')" width="130" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'SystemHealth' })

import { ref, onMounted } from 'vue'
import {
  getSystemHealthOverview,
  getSecurityAuditSummary,
  getUserPermissionMatrix,
  type SystemHealthOverview,
  type SecurityAuditSummary,
  type UserPermissionMatrix,
} from '@/api/bpcs'

const loading = ref(false)
const overview = ref<SystemHealthOverview>({
  totalServers: 0,
  onlineServers: 0,
  offlineServers: 0,
  avgCpuUsage: 0,
  avgMemoryUsage: 0,
  avgDiskUsage: 0,
  activeJobs: 0,
  alertCount: 0,
  criticalAlertCount: 0,
  todayBackupSuccess: 0,
  todayBackupFailed: 0,
  complianceRate: 0,
})
const securityAudit = ref<SecurityAuditSummary[]>([])
const permissionMatrix = ref<UserPermissionMatrix[]>([])

async function load() {
  loading.value = true
  try {
    const [overviewRes, auditRes, matrixRes] = await Promise.all([
      getSystemHealthOverview(),
      getSecurityAuditSummary(),
      getUserPermissionMatrix(),
    ])
    overview.value = overviewRes
    securityAudit.value = auditRes
    permissionMatrix.value = matrixRes
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
