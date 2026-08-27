<template>
  <div class="page-container page-container--fit">
    <!-- ── 搜索区 ───────────────────────────────────────── -->
    <div class="search-bar">
      <el-input
        v-model="query.cono"
        class="w-120"
        :placeholder="$t('bpcs.label.cono')"
        clearable
        @keyup.enter="search"
      />
      <el-input
        v-model="query.orno"
        class="w-200"
        :placeholder="$t('bpcs.label.orno')"
        clearable
        @keyup.enter="search"
      />
      <el-button type="primary" :loading="loading" @click="search">
        {{ $t('common.search') }}
      </el-button>
      <span class="hint">{{ $t('bpcs.searchHint') }}</span>
    </div>

    <div v-if="searched" class="table-wrapper">
      <!-- ── 订单头卡片 ──────────────────────────────── -->
      <el-card v-if="header" shadow="never" class="mb16">
        <template #header>
          <div class="flex-row-center">
            <span>
              {{ $t('bpcs.label.orderNo') }}: <b>{{ header.cono }}-{{ header.orno }}</b>
            </span>
            <el-tag size="small" type="info" class="ml8">{{ stageLabel(header.currentStageIndex) }}</el-tag>
            <el-tag v-if="closed" size="small" type="warning" class="ml8">
              {{ $t('bpcs.stage.closed') }}
            </el-tag>
          </div>
        </template>
        <el-descriptions :column="4" size="small">
          <el-descriptions-item :label="$t('bpcs.label.customer')">
            {{ header.customerNo || '—' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.label.shipTo')">
            {{ header.shipTo || '—' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.label.orderDate')">
            {{ header.orderDate || '—' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.label.reqDate')">
            {{ header.reqDate || '—' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.label.lineCount')">
            {{ header.lineCount }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.label.rawStatus')">
            CHSTS={{ header.raw.chsts }}
            <template v-if="header.raw.hstat"> / HSTAT={{ header.raw.hstat }}</template>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- ── 进程时间轴 ──────────────────────────────── -->
      <el-card v-if="header" shadow="never" class="mb16">
        <template #header>{{ $t('bpcs.timeline.title') }}</template>
        <OrderTimeline :nodes="header.timeline" />
      </el-card>

      <!-- ── 订单行表格（数量列随阶段动态呈现） ────────── -->
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table
          v-if="lines.length"
          :data="lines"
          size="small"
          border
          highlight-current-row
          @row-click="(row: BpcsOrderLine) => openDetail(row)"
        >
        <el-table-column prop="orln" :label="$t('bpcs.line.orln')" width="70" />
        <el-table-column prop="item" :label="$t('bpcs.line.item')" min-width="120" show-overflow-tooltip />
        <el-table-column prop="wh" :label="$t('bpcs.line.wh')" width="80" />
        <el-table-column align="right" :label="$t('bpcs.line.qtyOrdered')" width="90">
          <template #default="{ row }">{{ fmtQty(row.qtyOrdered) }}</template>
        </el-table-column>
        <!-- 分货数量：拣货释放后产生 -->
        <el-table-column
          v-if="showAllocated"
          align="right"
          :label="$t('bpcs.line.qtyAllocated')"
          width="90"
        >
          <template #default="{ row }">{{ fmtQty(row.qtyAllocated) }}</template>
        </el-table-column>
        <!-- 发货数量：拣货确认后产生 -->
        <el-table-column
          v-if="showShipped"
          align="right"
          :label="$t('bpcs.line.qtyShipped')"
          width="90"
        >
          <template #default="{ row }">{{ fmtQty(row.qtyShipped) }}</template>
        </el-table-column>
        <!-- 开票数量与金额：开票后产生 -->
        <template v-if="showInvoiced">
          <el-table-column align="right" :label="$t('bpcs.line.qtyInvoiced')" width="90">
            <template #default="{ row }">{{ fmtQty(row.qtyInvoiced) }}</template>
          </el-table-column>
          <el-table-column align="right" :label="$t('bpcs.line.price')" width="90">
            <template #default="{ row }">{{ fmtAmount(row.price) }}</template>
          </el-table-column>
        </template>
        <el-table-column
          align="right"
          :label="$t('bpcs.line.reqDate')"
          width="110"
          prop="reqDate"
        />
        <el-table-column :label="$t('bpcs.line.status')" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="stageTagType(row.stageIndex)">
              {{ stageLabel(row.stageIndex) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="" width="80" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row as BpcsOrderLine)">
              {{ $t('common.detail') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>

      <el-empty
        v-if="searched && !loading && lines.length === 0 && !notFound"
        :description="$t('common.noData')"
      />
      <el-empty v-if="notFound" :description="$t('bpcs.notFound')" />
    </div>

    <!-- ── 行详情抽屉 ──────────────────────────────── -->
    <el-drawer
      v-model="detailVisible"
      :title="$t('bpcs.detail.title') + (detailRow ? ' · ' + detailRow.orln : '')"
      size="560px"
      :close-on-click-modal="false"
    >
      <template v-if="detailRow">
        <h4 class="section">{{ $t('bpcs.detail.statusGroup') }}</h4>
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item :label="$t('bpcs.line.status')">
            <el-tag size="small" :type="stageTagType(detailRow.stageIndex)">
              {{ stageLabel(detailRow.stageIndex) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="CLSTS(raw)">
            {{ detailRow.rawClsts }}
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="section">{{ $t('bpcs.detail.itemGroup') }}</h4>
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item :label="$t('bpcs.line.item')">
            {{ detailRow.item || '—' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.line.itemDesc')">
            {{ detailRow.itemDesc || '—' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.line.wh')">
            {{ detailRow.wh || '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="section">{{ $t('bpcs.detail.qtyGroup') }}</h4>
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item :label="$t('bpcs.line.qtyOrdered')">
            {{ fmtQty(detailRow.qtyOrdered) }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.line.qtyAllocated')">
            {{ fmtQty(detailRow.qtyAllocated) }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.line.qtyShipped')">
            {{ fmtQty(detailRow.qtyShipped) }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.line.qtyInvoiced')">
            {{ fmtQty(detailRow.qtyInvoiced) }}
          </el-descriptions-item>
        </el-descriptions>
        <p class="hint">{{ $t('bpcs.detail.qtyHint') }}</p>

        <h4 class="section">{{ $t('bpcs.detail.allocationGroup') }}</h4>
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item :label="$t('bpcs.detail.ordered')">
            {{ fmtQty(detailRow.qtyOrdered) }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.detail.allocated')">
            {{ fmtQty(detailRow.qtyAllocated) }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.detail.pending')">
            {{ fmtPending(detailRow.qtyOrdered, detailRow.qtyAllocated) }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.detail.fulfillRate')">
            {{ fmtFulfillRate(detailRow.qtyOrdered, detailRow.qtyAllocated) }}
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="section">{{ $t('bpcs.detail.priceGroup') }}</h4>
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item :label="$t('bpcs.line.price')">
            {{ fmtAmount(detailRow.price) }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.line.discPct')">
            {{ fmtPct(detailRow.discPct) }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.detail.lineAmount')">
            {{ fmtLineAmount(detailRow) }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.line.reqDate')">
            {{ detailRow.reqDate || '—' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import RxSkeleton from '@/components/RxSkeleton.vue'
import {
  getOrderHeader,
  getOrderLines,
  type BpcsOrderHeader,
  type BpcsOrderLine,
} from '@/api/bpcs'

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsOrder' })

const { t } = useI18n()

const query = reactive({ cono: '001', orno: '' })
const loading = ref(false)
const searched = ref(false)
const notFound = ref(false)
const header = ref<BpcsOrderHeader | null>(null)
const lines = ref<BpcsOrderLine[]>([])

const detailVisible = ref(false)
const detailRow = ref<BpcsOrderLine | null>(null)

/** 已关闭（HID=CZ） */
const closed = computed(() => header.value?.raw.hid === 'CZ')

/**
 * 数量列组随订单当前阶段动态呈现（实际业务：CHSTS3/4 恒 0，
 * 流程为 录入 → 拣货释放 → 拣货确认 → 开票）：
 * - 分货数量：拣货释放(≥1)后可见；
 * - 发货数量：拣货确认(≥2)后可见；
 * - 开票数量+价格：开票(=3)后可见。
 */
const showAllocated = computed(() => (header.value?.currentStageIndex ?? 0) >= 1)
const showShipped = computed(() => (header.value?.currentStageIndex ?? 0) >= 2)
const showInvoiced = computed(() => (header.value?.currentStageIndex ?? 0) >= 3)

function search() {
  if (!query.cono.trim() || !query.orno.trim()) {
    ElMessage.warning(t('bpcs.searchRequired'))
    return
  }
  loading.value = true
  notFound.value = false
  header.value = null
  lines.value = []
    Promise.all([
      getOrderHeader(query.cono.trim(), query.orno.trim()),
      getOrderLines(query.cono.trim(), query.orno.trim()),
    ])
      .then(([h, l]) => {
        header.value = h
        lines.value = l
      })
      .catch(() => {
        // 错误 toast 由拦截器统一处理；此处仅收敛 loading 态
      })
      .finally(() => {
        loading.value = false
        searched.value = true
      })
  }

function openDetail(row: BpcsOrderLine) {
  detailRow.value = row
  detailVisible.value = true
}

/** 数量展示：未产生的阶段显示 “—” */
function fmtQty(v: number | null): string {
  return v == null ? '—' : String(v)
}

function fmtAmount(v: number | null): string {
  return v == null ? '—' : v.toLocaleString(undefined, { minimumFractionDigits: 2 })
}

function fmtPct(v: number | null): string {
  return v == null ? '—' : `${v}%`
}

/** 未分配数量 = 订购量 - 已分配量 */
function fmtPending(ordered: number | null, allocated: number | null): string {
  if (ordered == null) return '—'
  const a = allocated ?? 0
  const pending = ordered - a
  return pending > 0 ? String(pending) : '0'
}

/** 分配完成率 */
function fmtFulfillRate(ordered: number | null, allocated: number | null): string {
  if (ordered == null || ordered === 0) return '—'
  const a = allocated ?? 0
  const pct = Math.round((a / ordered) * 100)
  return `${pct}%`
}

/** 行金额 = 单价 × 订购量 × (1 - 折扣率) */
function fmtLineAmount(row: BpcsOrderLine): string {
  if (row.price == null || row.qtyOrdered == null) return '—'
  const disc = row.discPct != null ? row.discPct / 100 : 0
  const amount = row.price * row.qtyOrdered * (1 - disc)
  return fmtAmount(amount)
}

/** 阶段文案（复用时间轴词表 bpcs.stage.*） */
function stageLabel(idx: number): string {
  const keys = ['created', 'pick_released', 'pick_confirmed', 'billed', 'closed']
  return t(`bpcs.stage.${keys[idx] ?? 'created'}`)
}

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
function stageTagType(idx: number): TagType {
  if (idx >= 3) return 'success'
  if (idx >= 1) return 'primary'
  return 'info'
}
</script>

<style scoped>
/* toolbar/hint/宽度工具类已收敛至 src/styles/common.css（.w-120/.w-200） */
</style>
