<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" class="w-100" :placeholder="$t('bpcs.label.cono')" clearable @keyup.enter="load" />
      <el-button type="warning" :loading="loading" @click="load">
        {{ $t('common.search') }}
      </el-button>
      <span class="hint">{{ $t('bpcs.inventoryAlert.hint') }}</span>
    </div>

    <!-- 汇总卡片 -->
    <div v-if="alerts.length" class="summary-cards mb16">
      <div class="summary-card summary-card--danger">
        <div class="summary-value">{{ alerts.length }}</div>
        <div class="summary-label">{{ $t('bpcs.inventoryAlert.alertCount') }}</div>
      </div>
      <div class="summary-card summary-card--danger">
        <div class="summary-value">{{ totalDeficit }}</div>
        <div class="summary-label">{{ $t('bpcs.inventoryAlert.totalDeficit') }}</div>
      </div>
    </div>

    <div class="table-wrapper">
      <el-table :data="alerts" v-loading="loading" size="small" border row-key="item">
        <el-table-column prop="item" :label="$t('bpcs.line.item')" min-width="120" />
        <el-table-column prop="description" :label="$t('bpcs.line.itemDesc')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="warehouse" :label="$t('bpcs.label.wh')" width="80" />
        <el-table-column prop="uom" :label="$t('bpcs.inventory.uom')" width="60" />
        <el-table-column align="right" :label="$t('bpcs.inventory.onHand')" width="90">
          <template #default="{ row }">{{ row.onHand }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.inventory.allocated')" width="90">
          <template #default="{ row }">{{ row.allocated }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.inventory.available')" width="90">
          <template #default="{ row }">
            <span class="text-danger">{{ row.available }}</span>
          </template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.inventoryAlert.safetyStock')" width="100">
          <template #default="{ row }">{{ row.safetyStock }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.inventoryAlert.deficit')" width="90">
          <template #default="{ row }">
            <span class="text-danger font-bold">-{{ row.deficit }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && alerts.length === 0" :description="$t('bpcs.inventoryAlert.allGood')" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsInventoryAlert' })

import { computed, ref } from 'vue'
import { fetchInventoryAlerts, type InventoryAlert } from '@/api/supplyChain'

const cono = ref('001')
const loading = ref(false)
const alerts = ref<InventoryAlert[]>([])

const totalDeficit = computed(() => alerts.value.reduce((s, a) => s + a.deficit, 0))

function load() {
  loading.value = true
  fetchInventoryAlerts({ cono: cono.value || '001', limit: 100 })
    .then(data => { alerts.value = data })
    .finally(() => { loading.value = false })
}
</script>

<style scoped>
.summary-cards {
  display: flex;
  gap: 16px;
}
.summary-card {
  flex: 1;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}
.summary-card--danger {
  border-color: var(--color-danger);
  background: var(--el-color-danger-light-9);
}
.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}
.summary-card--danger .summary-value {
  color: var(--color-danger);
}
.summary-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}
</style>
