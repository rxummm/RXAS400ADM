<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="searchItem" class="w-120" clearable :placeholder="$t('bpcs.common.itemCode')" @keyup.enter="load" />
      <el-radio-group v-model="mode" class="ml8">
        <el-radio-button value="parents">{{ $t('bpcs.bom.findParents') }}</el-radio-button>
        <el-radio-button value="children">{{ $t('bpcs.bom.expandChildren') }}</el-radio-button>
      </el-radio-group>
      <el-button type="primary" @click="load" class="ml8">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="parent" :label="$t('bpcs.bom.parentItem')" width="130" />
        <el-table-column prop="component" :label="$t('bpcs.bom.component')" width="130" />
        <el-table-column prop="description" :label="$t('bpcs.common.description')" min-width="160" />
        <el-table-column prop="qty" :label="$t('bpcs.bom.quantity')" width="90" align="right" />
        <el-table-column prop="uom" :label="$t('bpcs.bom.uom')" width="70" align="center" />
        <el-table-column prop="onHandQty" :label="$t('bpcs.inventory.onHand')" width="90" align="right" />
        <el-table-column prop="availQty" :label="$t('bpcs.inventory.available')" width="90" align="right" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsBom' })

import { ref } from 'vue'
import { findBomParents, expandBomChildren, type BomLine } from '@/api/bpcs'

const searchItem = ref('')
const mode = ref<'parents' | 'children'>('children')
const loading = ref(false)
const rows = ref<BomLine[]>([])

const load = async () => {
  if (!searchItem.value) return
  loading.value = true
  try {
    if (mode.value === 'parents') {
      rows.value = await findBomParents('001', searchItem.value)
    } else {
      rows.value = await expandBomChildren('001', searchItem.value)
    }
  } finally {
    loading.value = false
  }
}
</script>
