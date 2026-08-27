<template>
  <div class="order-timeline">
    <el-timeline>
      <el-timeline-item
        v-for="(node, idx) in nodes"
        :key="node.stageKey"
        :type="timelineType(node, idx)"
        :hollow="!node.reached"
        :timestamp="node.timestamp || undefined"
        :class="{ 'is-current': node.current }"
      >
        <span class="stage-name" :class="{ reached: node.reached, current: node.current }">
          {{ $t(node.nameKey) }}
        </span>
        <span v-if="node.reached && !node.timestamp" class="done-mark">✓</span>
        <span v-if="!node.reached" class="pending-mark">{{ $t('bpcs.timeline.pending') }}</span>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup lang="ts">
import type { TimelineNode } from '@/api/bpcs'

defineProps<{
  nodes: TimelineNode[]
}>()

/**
 * 节点配色：已达成 success / 当前节点 primary(高亮) / 未达成灰
 * （时间轴为「里程碑达成态」——BPCS 仅存 0/1 标志位，多数节点无原生时刻）
 */
function timelineType(node: TimelineNode, _idx: number): 'primary' | 'success' | 'info' {
  if (node.current) return 'primary'
  return node.reached ? 'success' : 'info'
}
</script>

<style scoped>
.order-timeline {
  padding: 8px 4px 0 4px;
}
.stage-name {
  font-size: 13px;
  color: var(--text-secondary);
}
.stage-name.reached {
  color: var(--text-primary);
  font-weight: 600;
}
.stage-name.current {
  color: var(--color-primary);
}
.done-mark {
  margin-left: 6px;
  color: var(--color-success);
  font-weight: 700;
}
.pending-mark {
  margin-left: 6px;
  font-size: 12px;
  color: var(--text-placeholder);
}
.is-current :deep(.el-timeline-item__timestamp) {
  color: var(--color-primary);
}
</style>
