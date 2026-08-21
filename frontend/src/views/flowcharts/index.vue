<template>
  <div class="page-container page-container--fit">
    <div class="table-wrapper">
      <iframe :src="docsUrl" class="docs-frame" :title="$t('menu.flowcharts')" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

defineOptions({ name: 'Flowcharts' })

const DOCS_BASE = (import.meta.env.VITE_DOCS_URL as string | undefined) || 'http://localhost:5174'

// 流程图单页地址（docs 站为独立 VitePress 站点；dev 下 frontend 占 5173 时 docs 自动落 5174）
const docsUrl = computed(() => `${DOCS_BASE}/flowcharts/modules-mermaid-source`)
</script>

<style scoped>
.docs-frame {
  flex: 1;
  min-height: 0;
  width: 100%;
  border: none;
}
</style>
