<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="lotno" :placeholder="$t('bpcs.wms.lotno')" clearable class="search-bar__input" @keyup.enter="load" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <ExportDropdown :data="rows" :columns="exportColumns" :title="$t('bpcs.wms.batchTracking')" />
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="whse" :label="$t('bpcs.wms.whse')" width="80" />
        <el-table-column prop="binno" :label="$t('bpcs.wms.binno')" width="120" />
        <el-table-column prop="item" :label="$t('bpcs.label.item')" width="120" />
        <el-table-column prop="itdsc" :label="$t('bpcs.description')" />
        <el-table-column prop="qty" :label="$t('bpcs.quantity')" width="80" />
        <el-table-column prop="lotno" :label="$t('bpcs.wms.lotno')" width="120" />
        <el-table-column prop="trndate" :label="$t('bpcs.wms.trndate')" width="100" />
        <el-table-column prop="ittyp" :label="$t('bpcs.wms.trntype')" width="80" />
        <el-table-column prop="trnqty" :label="$t('bpcs.wms.trnqty')" width="80" />
        <el-table-column prop="refno" :label="$t('bpcs.wms.refno')" width="100" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { searchBatches, type BatchTracking } from '@/api/wms'
import ExportDropdown from '@/components/ExportDropdown.vue'

const { t } = useI18n()

const lotno = ref('')
const rows = ref<BatchTracking[]>([])
const loading = ref(false)
const exportColumns = computed(() => [
  { key: 'whse', label: t('bpcs.wms.whse') },
  { key: 'binno', label: t('bpcs.wms.binno') },
  { key: 'item', label: t('bpcs.label.item') },
  { key: 'itdsc', label: t('bpcs.description') },
  { key: 'qty', label: t('bpcs.quantity') },
  { key: 'lotno', label: t('bpcs.wms.lotno') },
  { key: 'trndate', label: t('bpcs.wms.trndate') },
  { key: 'ittyp', label: t('bpcs.wms.trntype') },
  { key: 'trnqty', label: t('bpcs.wms.trnqty') },
  { key: 'refno', label: t('bpcs.wms.refno') }
])

async function load() {
  loading.value = true
  try {
    const res = await searchBatches({ cono: '001', lotno: lotno.value, current: 1, size: 1000 })
    rows.value = res.records
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
