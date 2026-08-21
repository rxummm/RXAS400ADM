<template>
  <el-tag
    :type="tagType"
    :effect="effect"
    :size="size"
    :class="['status-badge', `status-badge--${status}`]"
    :disable-transitions="false"
  >
    <el-icon v-if="showIcon && icon" :size="iconSize" class="status-badge__icon">
      <component :is="icon" />
    </el-icon>
    <span v-if="showDot" class="status-badge__dot" />
    <span v-if="showLabel" class="status-badge__label">{{ label }}</span>
    <slot />
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'
import {
  CircleCheckFilled,
  WarningFilled,
  CircleCloseFilled,
  InfoFilled,
  Loading,
} from '@element-plus/icons-vue'

/**
 * StatusBadge - 语义化状态标签
 *
 * 标准化运维系统状态颜色映射：
 * - healthy/success/online/enabled → 绿色
 * - warning/busy/partial → 黄色
 * - critical/error/offline/disabled → 红色
 * - unknown/pending → 灰色
 *
 * 用法：
 * <StatusBadge status="healthy" label="正常" />
 * <StatusBadge status="critical" :show-dot="true" />
 * <StatusBadge status="warning" size="large" />
 */

export type StatusType =
  | 'healthy' | 'success' | 'online' | 'enabled'
  | 'warning' | 'busy' | 'partial'
  | 'critical' | 'error' | 'offline' | 'disabled'
  | 'unknown' | 'pending' | 'loading'

const props = withDefaults(defineProps<{
  /** 状态类型 */
  status: StatusType
  /** 显示标签文字 */
  label?: string
  /** 显示模式 */
  mode?: 'tag' | 'dot' | 'icon' | 'text'
  /** 标签大小 */
  size?: 'large' | 'default' | 'small'
  /** 标签效果 */
  effect?: 'dark' | 'light' | 'plain'
  /** 是否显示图标 */
  showIcon?: boolean
  /** 是否显示圆点 */
  showDot?: boolean
  /** 是否显示标签文字 */
  showLabel?: boolean
}>(), {
  label: '',
  mode: 'tag',
  size: 'small',
  effect: 'light',
  showIcon: false,
  showDot: false,
  showLabel: true,
})

// 状态到 Element Plus tag type 的映射
const tagType = computed(() => {
  const map: Record<string, string> = {
    healthy: 'success',
    success: 'success',
    online: 'success',
    enabled: 'success',
    warning: 'warning',
    busy: 'warning',
    partial: 'warning',
    critical: 'danger',
    error: 'danger',
    offline: 'danger',
    disabled: 'danger',
    unknown: 'info',
    pending: 'info',
    loading: 'info',
  }
  return map[props.status] || 'info'
})

// 状态到图标的映射
const icon = computed(() => {
  const map: Record<string, Component> = {
    healthy: CircleCheckFilled,
    success: CircleCheckFilled,
    online: CircleCheckFilled,
    enabled: CircleCheckFilled,
    warning: WarningFilled,
    busy: WarningFilled,
    partial: WarningFilled,
    critical: CircleCloseFilled,
    error: CircleCloseFilled,
    offline: CircleCloseFilled,
    disabled: CircleCloseFilled,
    unknown: InfoFilled,
    pending: InfoFilled,
    loading: Loading,
  }
  return map[props.status]
})

const iconSize = computed(() => {
  const map = { large: 16, default: 14, small: 12 }
  return map[props.size]
})

// 默认标签文字
const label = computed(() => {
  if (props.label) return props.label
  const map: Record<string, string> = {
    healthy: '正常',
    success: '成功',
    online: '在线',
    enabled: '启用',
    warning: '警告',
    busy: '忙碌',
    partial: '部分',
    critical: '严重',
    error: '错误',
    offline: '离线',
    disabled: '禁用',
    unknown: '未知',
    pending: '等待中',
    loading: '加载中',
  }
  return map[props.status] || props.status
})
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.status-badge__icon {
  margin-right: 2px;
}

.status-badge__label {
  font-size: inherit;
}

.status-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: currentColor;
}

/* 状态特定样式 */
.status-badge--healthy,
.status-badge--success,
.status-badge--online,
.status-badge--enabled {
  --el-tag-bg-color: var(--color-success-light-9, #f0f9eb);
  --el-tag-border-color: var(--color-success-light-5, #e1f3d8);
  --el-tag-text-color: var(--color-success, #67c23a);
}

.status-badge--warning,
.status-badge--busy,
.status-badge--partial {
  --el-tag-bg-color: var(--color-warning-light-9, #fdf6ec);
  --el-tag-border-color: var(--color-warning-light-5, #faecd8);
  --el-tag-text-color: var(--color-warning, #e6a23c);
}

.status-badge--critical,
.status-badge--error,
.status-badge--offline,
.status-badge--disabled {
  --el-tag-bg-color: var(--color-danger-light-9, #fef0f0);
  --el-tag-border-color: var(--color-danger-light-5, #fde2e2);
  --el-tag-text-color: var(--color-danger, #f56c6c);
}

.status-badge--unknown,
.status-badge--pending,
.status-badge--loading {
  --el-tag-bg-color: var(--color-info-light-9, #f4f4f5);
  --el-tag-border-color: var(--color-info-light-5, #e9e9eb);
  --el-tag-text-color: var(--color-info, #909399);
}

/* 暗色主题适配 */
html.dark .status-badge--healthy,
html.dark .status-badge--success {
  --el-tag-bg-color: rgba(103, 194, 58, 0.15);
  --el-tag-border-color: rgba(103, 194, 58, 0.3);
}

html.dark .status-badge--warning,
html.dark .status-badge--busy {
  --el-tag-bg-color: rgba(230, 162, 60, 0.15);
  --el-tag-border-color: rgba(230, 162, 60, 0.3);
}

html.dark .status-badge--critical,
html.dark .status-badge--error {
  --el-tag-bg-color: rgba(245, 108, 108, 0.15);
  --el-tag-border-color: rgba(245, 108, 108, 0.3);
}
</style>