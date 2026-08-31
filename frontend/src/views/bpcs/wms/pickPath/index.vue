<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="whse" :placeholder="$t('bpcs.wms.whse')" clearable class="search-bar__input" @keyup.enter="load" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <ExportDropdown :data="pickList" :columns="exportColumns" :title="$t('bpcs.wms.pickPath')" />
    </div>
    <div class="table-wrapper">
      <el-table :data="pickList" v-loading="loading" size="small" border row-key="binno">
        <el-table-column type="index" :label="$t('bpcs.wms.pickOrder')" width="80" />
        <el-table-column prop="binno" :label="$t('bpcs.wms.binno')" width="120" />
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
const pickList = ref<BinInventory[]>([])
const loading = ref(false)
const exportColumns = computed(() => [
  { key: 'binno', label: t('bpcs.wms.binno') },
  { key: 'item', label: t('bpcs.label.item') },
  { key: 'itdsc', label: t('bpcs.description') },
  { key: 'qty', label: t('bpcs.quantity') },
  { key: 'lotno', label: t('bpcs.wms.lotno') }
])

async function load() {
  loading.value = true
  try {
    const res = await searchBinInventory({ cono: '001', whse: whse.value, current: 1, size: 1000 })
    pickList.value = (res.records || []).sort((a, b) => a.binno.localeCompare(b.binno))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
