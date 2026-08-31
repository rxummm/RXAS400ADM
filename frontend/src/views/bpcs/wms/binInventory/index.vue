<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="whse" :placeholder="$t('bpcs.wms.whse')" clearable class="search-bar__input" @keyup.enter="load" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <ExportDropdown :data="rows" :columns="exportColumns" :title="$t('bpcs.wms.binInventory')" />
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="whse" :label="$t('bpcs.wms.whse')" width="80" />
        <el-table-column prop="binno" :label="$t('bpcs.wms.binno')" width="120" />
        <el-table-column prop="bintype" :label="$t('bpcs.wms.bintype')" width="80" />
        <el-table-column prop="status" :label="$t('bpcs.wms.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'O' ? 'primary' : row.status === 'F' ? 'success' : 'info'" size="small">
              {{ row.status === 'O' ? $t('bpcs.wms.occupied') : row.status === 'F' ? $t('bpcs.wms.freeTag') : row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="capacity" :label="$t('bpcs.wms.capacity')" width="80" />
        <el-table-column prop="item" :label="$t('bpcs.label.item')" width="120" />
        <el-table-column prop="itdsc" :label="$t('bpcs.description')" />
        <el-table-column prop="qty" :label="$t('bpcs.quantity')" width="80" />
        <el-table-column prop="lotno" :label="$t('bpcs.wms.lotno')" width="120" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { searchBinInventory, type BinInventory } from '@/api/wms'
import ExportDropdown from '@/components/ExportDropdown.vue'

const { t } = useI18n()

const whse = ref('')
const rows = ref<BinInventory[]>([])
const loading = ref(false)
const exportColumns = computed(() => [
  { key: 'whse', label: t('bpcs.wms.whse') },
  { key: 'binno', label: t('bpcs.wms.binno') },
  { key: 'bintype', label: t('bpcs.wms.bintype') },
  { key: 'status', label: t('bpcs.wms.status') },
  { key: 'item', label: t('bpcs.label.item') },
  { key: 'itdsc', label: t('bpcs.description') },
  { key: 'qty', label: t('bpcs.quantity') },
  { key: 'lotno', label: t('bpcs.wms.lotno') }
])

async function load() {
  loading.value = true
  try {
    const res = await searchBinInventory({ cono: '001', whse: whse.value, current: 1, size: 1000 })
    rows.value = res.records
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
