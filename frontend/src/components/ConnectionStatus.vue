<template>
  <div class="connection-status" :class="[`connection-status--${state}`]">
    <el-tooltip :content="tooltipText" placement="bottom">
      <div class="connection-status__indicator" @click="$emit('click')">
        <span class="connection-status__dot" />
        <span v-if="showLabel" class="connection-status__label">{{ label }}</span>
        <span v-if="showFreshness && lastUpdate" class="connection-status__freshness">
          {{ freshnessText }}
        </span>
      </div>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

/**
 * ConnectionStatus - WebSocket 连接状态指示器
 *
 * 显示：
 * - 连接状态（绿=已连接、黄=重连中、红=断开、灰=未连接）
 * - 数据新鲜度（Last updated: 3s ago）
 * - 点击可手动重连
 *
 * 用法：
 * <ConnectionStatus
 *   :state="wsState"
 *   :last-update="lastDataTime"
 *   @click="reconnect"
 * />
 */

export type ConnectionState = 'connected' | 'reconnecting' | 'disconnected' | 'idle'

const props = withDefaults(defineProps<{
  /** 连接状态 */
  state?: ConnectionState
  /** 最后数据更新时间 */
  lastUpdate?: Date | null
  /** 是否显示标签 */
  showLabel?: boolean
  /** 是否显示新鲜度 */
  showFreshness?: boolean
  /** 新鲜度超时阈值（秒） */
  freshnessThreshold?: number
}>(), {
  state: 'idle',
  lastUpdate: null,
  showLabel: true,
  showFreshness: true,
  freshnessThreshold: 30,
})

defineEmits<{
  click: []
}>()

const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const label = computed(() => {
  const map: Record<ConnectionState, string> = {
    connected: '已连接',
    reconnecting: '重连中',
    disconnected: '已断开',
    idle: '未连接',
  }
  return map[props.state]
})

const freshnessText = computed(() => {
  if (!props.lastUpdate) return ''
  const seconds = Math.floor((now.value - props.lastUpdate.getTime()) / 1000)
  if (seconds < 5) return '实时'
  if (seconds < 60) return `${seconds}s 前`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m 前`
  return `${Math.floor(seconds / 3600)}h 前`
})

const isStale = computed(() => {
  if (!props.lastUpdate) return false
  const seconds = (now.value - props.lastUpdate.getTime()) / 1000
  return seconds > props.freshnessThreshold
})

const tooltipText = computed(() => {
  const parts = [`连接状态: ${label.value}`]
  if (props.lastUpdate) {
    parts.push(`最后更新: ${freshnessText.value}`)
    if (isStale.value) {
      parts.push('⚠️ 数据可能已过期')
    }
  }
  return parts.join('\n')
})
</script>

<style scoped>
.connection-status {
  display: inline-flex;
  align-items: center;
}

.connection-status__indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 999px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.connection-status__indicator:hover {
  background-color: var(--bg-hover);
}

.connection-status__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  transition: background-color 0.3s;
}

.connection-status__label {
  font-size: 12px;
  color: var(--text-secondary);
}

.connection-status__freshness {
  font-size: 11px;
  color: var(--text-placeholder);
  margin-left: 4px;
}

/* 状态样式 */
.connection-status--connected .connection-status__dot {
  background-color: var(--color-success);
  box-shadow: 0 0 6px var(--color-success);
}

.connection-status--reconnecting .connection-status__dot {
  background-color: var(--color-warning);
  animation: pulse 1.5s infinite;
}

.connection-status--disconnected .connection-status__dot {
  background-color: var(--color-danger);
}

.connection-status--idle .connection-status__dot {
  background-color: var(--color-info);
}

/* 脉冲动画 */
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* 暗色主题增强 */
html.dark .connection-status--connected .connection-status__dot {
  box-shadow: 0 0 8px rgba(103, 194, 58, 0.6);
}

html.dark .connection-status--disconnected .connection-status__dot {
  box-shadow: 0 0 8px rgba(245, 108, 108, 0.6);
}
</style>
