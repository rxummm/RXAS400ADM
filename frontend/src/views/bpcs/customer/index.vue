<template>
  <div class="page-container page-container--fit">
    <div class="customer-layout">
      <!-- ── 左侧：客户列表 ──────────────────────────── -->
      <div class="customer-list-panel">
        <div class="search-bar">
          <el-input
            v-model="keyword"
            :placeholder="$t('bpcs.customer.searchPlaceholder')"
            clearable
            @keyup.enter="search"
          />
          <el-button type="primary" size="small" @click="search">
            {{ $t('common.search') }}
          </el-button>
          <ExportDropdown
            :data="customers"
            :columns="exportColumns"
            :title="$t('bpcs.menu.customers')"
            :export-url="BPCS_EXPORT.customers"
            :query-params="{ name: keyword }"
          />
        </div>
        <div class="customer-list" v-loading="loading">
          <div
            v-for="c in customers"
            :key="c.cust"
            class="customer-item"
            :class="{ active: selectedCust?.cust === c.cust }"
            @click="selectCustomer(c)"
          >
            <div class="cust-name">{{ c.name || c.cust }}</div>
            <div class="cust-code">{{ c.cust }}</div>
          </div>
          <el-empty v-if="!loading && customers.length === 0" :image-size="60" />
        </div>
        <AppPagination v-if="total > 0" :total="total" v-model:current="current" v-model:size="size" @change="search" @size-change="search" />
      </div>

      <!-- ── 右侧：客户详情卡片 ──────────────────────── -->
      <div class="customer-detail-panel">
        <template v-if="selectedCust">
          <el-card shadow="never" class="mb16">
            <template #header>
              <div class="flex-row-center">
                <span class="mr4">{{ selectedCust.name || '—' }}</span>
                <el-tag size="small" type="info">{{ selectedCust.cust }}</el-tag>
              </div>
            </template>
            <el-descriptions :column="2" size="small" border>
              <el-descriptions-item :label="$t('bpcs.customer.address')">
                {{ selectedCust.address1 || '—' }}
                <template v-if="selectedCust.address2"> {{ selectedCust.address2 }}</template>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('bpcs.customer.city')">
                {{ [selectedCust.city, selectedCust.state, selectedCust.zip].filter(Boolean).join(' ') || '—' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('bpcs.customer.phone')">
                {{ selectedCust.phone || '—' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('bpcs.customer.contact')">
                {{ selectedCust.contact || '—' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('bpcs.customer.creditLimit')">
                {{ selectedCust.creditLimit != null ? selectedCust.creditLimit.toLocaleString() : '—' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('bpcs.customer.termsCode')">
                {{ selectedCust.termsCode || '—' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('bpcs.customer.taxCode')">
                {{ selectedCust.taxCode || '—' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('bpcs.customer.salesArea')">
                {{ selectedCust.salesArea || '—' }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- Ship-To 收货点 -->
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.customer.shipToTitle') }} ({{ selectedCust.shipTos?.length || 0 }})</template>
            <el-table :data="selectedCust.shipTos || []" size="small" border>
              <el-table-column prop="ship" :label="$t('bpcs.label.shipTo')" width="80" />
              <el-table-column prop="name" :label="$t('bpcs.customer.shipName')" min-width="120" show-overflow-tooltip />
              <el-table-column prop="address1" :label="$t('bpcs.customer.address')" min-width="160" show-overflow-tooltip />
              <el-table-column prop="city" :label="$t('bpcs.customer.city')" width="80" />
              <el-table-column prop="phone" :label="$t('bpcs.customer.phone')" width="130" />
            </el-table>
          </el-card>
        </template>
        <el-empty v-else :description="$t('bpcs.customer.selectHint')" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { searchCustomers, type BpcsCustomer, BPCS_EXPORT } from '@/api/bpcs'
import AppPagination from '@/components/AppPagination.vue'
import ExportDropdown from '@/components/ExportDropdown.vue'
import type { ExportColumn } from '@/components/ExportButton.vue'

defineOptions({ name: 'BpcsCustomer' })

const keyword = ref('')
const loading = ref(false)
const customers = ref<BpcsCustomer[]>([])
const total = ref(0)

const exportColumns: ExportColumn[] = [
  { key: 'cono', label: '公司' },
  { key: 'cust', label: '客户号' },
  { key: 'name', label: '客户名称' },
  { key: 'address1', label: '地址' },
  { key: 'city', label: '城市' },
  { key: 'state', label: '州' },
  { key: 'zip', label: '邮编' },
  { key: 'phone', label: '电话' },
  { key: 'contact', label: '联系人' },
  { key: 'creditLimit', label: '信用额度' },
]
const current = ref(1)
const size = ref(20)
const selectedCust = ref<BpcsCustomer | null>(null)

function search() {
  loading.value = true
  const params: Record<string, string | number> = { current: current.value, size: size.value }
  const kw = keyword.value.trim()
  if (kw) {
    // 简单策略：纯数字当客户号，否则当名称
    if (/^\d+$/.test(kw)) {
      params.cust = kw
    } else {
      params.name = kw
    }
  }
  searchCustomers(params)
    .then(data => {
      customers.value = data.records
      total.value = data.total
      if (data.records.length === 1) {
        selectedCust.value = data.records[0]
      }
    })
    .finally(() => { loading.value = false })
}

function selectCustomer(c: BpcsCustomer) {
  selectedCust.value = c
}
</script>

<style scoped>
.customer-layout {
  display: flex;
  gap: 16px;
  height: 100%;
  min-height: 0;
}
.customer-list-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}
.customer-list-panel .search-bar {
  padding: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  gap: 8px;
}
.customer-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px;
}
.customer-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 2px;
}
.customer-item:hover {
  background: var(--el-fill-color-light);
}
.customer-item.active {
  background: var(--el-color-primary-light-9);
  border-left: 3px solid var(--color-primary);
}
.cust-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.cust-code {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}
.customer-detail-panel {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
}
</style>
