<template>
  <el-dropdown trigger="click" @command="toggleColumn">
    <el-button>
      <el-icon><Setting /></el-icon> {{ $t('common.columns') }}
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-for="col in columns" :key="col.key" :command="col.key">
          <el-icon v-if="visible.includes(col.key)"><Check /></el-icon>
          <span :style="{ opacity: visible.includes(col.key) ? 1 : 0.4 }">{{ col.label }}</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { Check, Setting } from '@element-plus/icons-vue'

/**
 * 列显隐配置下拉（参照旧项目 TableColumnSettings）。
 * 用法：
 * const visibleColumns = ref<string[]>(columnOptions.map(c => c.key))
 * <TableColumnSettings :columns="columnOptions" v-model:visible="visibleColumns" />
 */
export interface ColumnOption {
  key: string
  label: string
}

defineProps<{ columns: ColumnOption[] }>()
const visible = defineModel<string[]>('visible', { required: true })

function toggleColumn(key: string) {
  const idx = visible.value.indexOf(key)
  if (idx > -1) {
    visible.value.splice(idx, 1)
  } else {
    visible.value.push(key)
  }
}
</script>
