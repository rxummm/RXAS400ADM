<template>
  <el-dialog :model-value="visible" :title="$t('layout.shortcuts')" width="480px" @close="close">
    <el-table :data="shortcuts" size="small">
      <el-table-column :label="$t('layout.shortcutKey')" width="160">
        <template #default="{ row }: { row: ShortcutItem }">
          <kbd class="kbd">{{ row.key }}</kbd>
        </template>
      </el-table-column>
      <el-table-column :label="$t('layout.shortcutDesc')">
        <template #default="{ row }: { row: ShortcutItem }">{{ row.description }}</template>
      </el-table-column>
    </el-table>
    <div class="hint">{{ $t('layout.shortcutHint') }}</div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

defineOptions({ name: 'ShortcutsHelp' })

const visible = ref(false)
const shortcuts = ref<ShortcutItem[]>([])

interface ShortcutItem {
  key: string
  description: string
}

function open(list: ShortcutItem[]) {
  shortcuts.value = list
  visible.value = true
}

function close() {
  visible.value = false
}

watch(visible, (v) => {
  if (!v) close()
})

defineExpose({ open, close, visible })
</script>

<style scoped>
.kbd {
  display: inline-block;
  padding: 1px 8px;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-hover);
  border: 1px solid var(--border-color);
  border-radius: 4px;
}
.hint {
  margin-top: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
