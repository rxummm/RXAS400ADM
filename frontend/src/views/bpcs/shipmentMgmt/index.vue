<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" class="w-120" clearable :placeholder="$t('bpcs.common.companyCode')" @keyup.enter="load" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="loadNo" :label="$t('bpcs.shipment.loadNo')" width="140" />
        <el-table-column prop="carrier" :label="$t('bpcs.shipment.carrier')" width="100" />
        <el-table-column prop="destination" :label="$t('bpcs.shipment.destination')" min-width="160" />
        <el-table-column prop="shipDate" :label="$t('bpcs.shipment.shipDate')" width="100" />
        <el-table-column prop="lineCount" :label="$t('bpcs.shipment.lineCount')" width="90" align="center" />
        <el-table-column prop="weight" :label="$t('bpcs.shipment.weight')" width="90" align="right" />
        <el-table-column prop="orderNos" :label="$t('bpcs.shipment.orderNos')" min-width="180" />
        <el-table-column :label="$t('common.actions')" width="120" align="center">
          <template #default="{ row }">
            <el-button size="small" link @click="handlePdf(row as ShipmentVO)">{{ $t('bpcs.shipment.pdf') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- PDF 预览对话框 -->
    <el-dialog v-model="pdfVisible" :title="$t('bpcs.shipment.pdfPreview')" width="90%" fullscreen>
      <iframe v-if="pdfUrl" :src="pdfUrl" class="pdf-iframe" sandbox="allow-same-origin allow-scripts" />
      <template #footer>
        <el-button @click="pdfVisible = false">{{ $t('common.close') }}</el-button>
        <el-button type="primary" @click="downloadPdf">{{ $t('common.download') }}</el-button>
        <el-button @click="printPdf">{{ $t('common.print') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsShipmentMgmt' })

import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { listShipments, exportShipmentPdf, type ShipmentVO } from '@/api/bpcs'
import { ElMessage } from 'element-plus'

const { t } = useI18n()
const cono = ref('001')
const loading = ref(false)
const rows = ref<ShipmentVO[]>([])
const pdfVisible = ref(false)
const pdfUrl = ref('')
const currentWaybillNo = ref('')

const load = async () => {
  loading.value = true
  try {
    rows.value = await listShipments(cono.value)
  } finally {
    loading.value = false
  }
}

const handlePdf = async (row: ShipmentVO) => {
  try {
    const params = {
      shipFrom: 'Company Warehouse',
      shipTo: row.destination,
      carrier: row.carrier,
      weight: row.weight,
      items: [{ itemCode: 'N/A', description: row.orderNos, qty: row.lineCount, unit: 'PCS' }]
    }
    const blob = await exportShipmentPdf(cono.value, row.loadNo, params).then(r => r.data)
    const url = URL.createObjectURL(blob)
    pdfUrl.value = url
    currentWaybillNo.value = row.loadNo
    pdfVisible.value = true
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    ElMessage.error(msg)
  }
}

const downloadPdf = () => {
  if (!pdfUrl.value) return
  const a = document.createElement('a')
  a.href = pdfUrl.value
  a.download = `waybill-${currentWaybillNo.value}.pdf`
  document.body.appendChild(a)
  a.click()
  a.remove()
}

const printPdf = () => {
  if (!pdfUrl.value) return
  const iframe = document.createElement('iframe')
  iframe.style.display = 'none'
  iframe.src = pdfUrl.value
  document.body.appendChild(iframe)
  iframe.onload = () => {
    iframe.contentWindow?.print()
  }
}

onMounted(load)
</script>

<style scoped>
.pdf-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>