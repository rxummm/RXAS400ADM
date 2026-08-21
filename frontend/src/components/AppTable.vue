<template>
  <div class="app-table" :class="[`app-table--${density}`]">
    <!-- 工具栏：列设置 + 批量操作 -->
    <div v-if="showToolbar" class="app-table__toolbar">
      <div class="app-table__toolbar-left">
        <slot name="toolbar-left" />
      </div>
      <div class="app-table__toolbar-right">
        <slot name="toolbar-right" />
        <TableColumnSettings
          v-if="showColumnSettings"
          :columns="columnOptions || []"
          v-model:visible="visibleColumns"
        />
        <el-dropdown v-if="density !== undefined" trigger="click" @command="setDensity">
          <el-button size="small">
            <el-icon><Grid /></el-icon>
            {{ $t('common.columns') }}
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="compact">{{ $t('table.densityCompact') }}</el-dropdown-item>
              <el-dropdown-item command="default">{{ $t('table.densityDefault') }}</el-dropdown-item>
              <el-dropdown-item command="comfortable">{{ $t('table.densityComfortable') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 表格主体 -->
    <div class="app-table__body" v-loading="loading">
      <el-table
        ref="tableRef"
        :data="data"
        :size="tableSize"
        :border="border"
        :stripe="stripe"
        :height="height"
        :max-height="maxHeight"
        :row-key="rowKey"
        :highlight-current-row="highlightCurrentRow"
        :empty-text="emptyText || $t('common.noData')"
        @selection-change="onSelectionChange"
        @sort-change="onSortChange"
        @row-click="onRowClick"
      >
        <!-- 选择列 -->
        <el-table-column
          v-if="selectable"
          type="selection"
          width="45"
          fixed="left"
        />

        <!-- 序号列 -->
        <el-table-column
          v-if="showIndex"
          type="index"
          :label="$t('common.index')"
          width="55"
          fixed="left"
        />

        <!-- 动态列 -->
        <slot />

        <!-- 操作列 -->
        <el-table-column
          v-if="$slots.actions"
          :label="$t('common.actions')"
          :width="actionsWidth"
          fixed="right"
          align="center"
        >
          <template #default="scope">
            <slot name="actions" v-bind="scope" />
          </template>
        </el-table-column>
      </el-table>

      <!-- 空态插槽 -->
      <div v-if="!loading && (!data || data.length === 0)" class="app-table__empty">
        <slot name="empty">
          <el-empty :description="emptyText || $t('common.noData')" :image-size="80" />
        </slot>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="showPagination && total > 0" class="app-table__pagination">
      <AppPagination
        :total="total"
        v-model:current="currentPage"
        v-model:size="pageSize"
        :show-sizes="showSizes"
        @change="emit('page-change')"
        @size-change="emit('size-change')"
      />
    </div>

    <!-- 批量操作栏 -->
    <transition name="slide-up">
      <div v-if="selectable && selectedRows.length > 0" class="app-table__bulk-bar">
        <span class="app-table__bulk-count">
          {{ $t('table.selected', { count: selectedRows.length }) }}
        </span>
        <slot name="bulk-actions" :selected="selectedRows" />
        <el-button size="small" @click="clearSelection">{{ $t('common.cancel') }}</el-button>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Grid } from '@element-plus/icons-vue'
import AppPagination from './AppPagination.vue'
import TableColumnSettings from './TableColumnSettings.vue'
import type { ColumnOption } from './TableColumnSettings.vue'

export type TableDensity = 'compact' | 'default' | 'comfortable'

const props = withDefaults(defineProps<{
  /** 表格数据 */
  data: Record<string, unknown>[]
  /** 是否加载中 */
  loading?: boolean
  /** 总条数（分页用） */
  total?: number
  /** 是否显示分页 */
  showPagination?: boolean
  /** 是否显示每页条数选择 */
  showSizes?: boolean
  /** 是否可选择（复选框） */
  selectable?: boolean
  /** 是否显示序号列 */
  showIndex?: boolean
  /** 是否显示边框 */
  border?: boolean
  /** 是否显示斑马纹 */
  stripe?: boolean
  /** 表格高度 */
  height?: string | number
  /** 表格最大高度 */
  maxHeight?: string | number
  /** 行数据的唯一标识字段名 */
  rowKey?: string
  /** 是否高亮当前行 */
  highlightCurrentRow?: boolean
  /** 操作列宽度 */
  actionsWidth?: number
  /** 空态提示文字 */
  emptyText?: string
  /** 是否显示工具栏 */
  showToolbar?: boolean
  /** 是否显示列设置 */
  showColumnSettings?: boolean
  /** 列配置（用于列设置） */
  columnOptions?: ColumnOption[]
  /** 表格密度 */
  density?: TableDensity
}>(), {
  loading: false,
  total: 0,
  showPagination: true,
  showSizes: true,
  selectable: false,
  showIndex: false,
  border: true,
  stripe: false,
  height: undefined,
  maxHeight: undefined,
  emptyText: undefined,
  columnOptions: () => [],
  rowKey: 'id',
  highlightCurrentRow: false,
  actionsWidth: 120,
  showToolbar: true,
  showColumnSettings: false,
  density: 'default',
})

const emit = defineEmits<{
  'page-change': []
  'size-change': []
  'selection-change': [rows: Record<string, unknown>[]]
  'sort-change': [column: Record<string, unknown>, prop: string, order: string]
  'row-click': [row: Record<string, unknown>, column: Record<string, unknown>, event: Event]
}>()

const tableRef = ref()
const currentPage = defineModel<number>('current', { default: 1 })
const pageSize = defineModel<number>('size', { default: 10 })
const visibleColumns = defineModel<string[]>('visibleColumns', { default: () => [] })
const selectedRows = ref<Record<string, unknown>[]>([])

const tableSize = computed(() => {
  const sizeMap: Record<TableDensity, string> = {
    compact: 'small',
    default: 'small',
    comfortable: 'default',
  }
  return sizeMap[props.density]
})

const onSelectionChange = (rows: Record<string, unknown>[]) => {
  selectedRows.value = rows
  emit('selection-change', rows)
}

const onSortChange = ({ column, prop, order }: { column: Record<string, unknown>; prop: string; order: string }) => {
  emit('sort-change', column, prop, order)
}

const onRowClick = (row: Record<string, unknown>, column: Record<string, unknown>, event: Event) => {
  emit('row-click', row, column, event)
}

const clearSelection = () => {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

const setDensity = (_newDensity: TableDensity) => {
  // 密度变更由父组件处理
}

// 从 columnOptions 初始化可见列
watch(() => props.columnOptions, (cols) => {
  if (cols && visibleColumns.value.length === 0) {
    visibleColumns.value = cols.map(c => c.key)
  }
}, { immediate: true })

defineExpose({
  tableRef,
  clearSelection,
  selectedRows,
})
</script>

<style scoped>
.app-table {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.app-table__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  flex-shrink: 0;
}

.app-table__toolbar-left,
.app-table__toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-table__body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.app-table__body :deep(.el-table) {
  height: 100%;
}

.app-table__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}

.app-table__pagination {
  flex-shrink: 0;
  padding: 8px 0;
}

/* 批量操作栏 */
.app-table__bulk-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: var(--color-primary-soft);
  border-radius: 6px;
  margin-top: 8px;
}

.app-table__bulk-count {
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 500;
}

/* 滑入动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.2s ease;
}
.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* 密度变体 */
.app-table--compact :deep(.el-table .el-table__cell) {
  padding: 4px 0;
}
.app-table--compact :deep(.el-table .cell) {
  padding: 0 8px;
}

.app-table--comfortable :deep(.el-table .el-table__cell) {
  padding: 12px 0;
}
.app-table--comfortable :deep(.el-table .cell) {
  padding: 0 16px;
}
</style>