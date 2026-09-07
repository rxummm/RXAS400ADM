<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="sourceOrder" clearable :placeholder="$t('bpcs.orderCopy.sourceOrder')" @keyup.enter="handleCopy" />
      <el-button type="primary" @click="handleCopy">{{ $t('bpcs.orderCopy.copy') }}</el-button>
    </div>
    <div class="table-wrapper" v-if="result">
      <el-alert :title="$t('bpcs.orderCopy.success')" type="success" show-icon :closable="false" class="mb16">
        <template #default>
          <span>{{ $t('bpcs.orderCopy.newOrderNo') }}: <strong>{{ result }}</strong></span>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsOrderCopy' })

import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { copyOrder } from '@/api/bpcs'

const { t } = useI18n()
const sourceOrder = ref('')
const result = ref<string | null>(null)

const handleCopy = async () => {
  if (!sourceOrder.value) {
    ElMessage.warning(t('common.validation.notBlank'))
    return
  }
  try {
    const res = await copyOrder({ cono: '001', sourceOrno: sourceOrder.value })
    result.value = res
    ElMessage.success(t('common.operationSuccess'))
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    ElMessage.error(msg)
  }
}
</script>
