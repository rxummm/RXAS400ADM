<template>
  <teleport to="body">
    <transition name="context-menu-fade">
      <div
        v-if="visible"
        class="context-menu"
        :style="menuStyle"
        @click="visible = false"
        @contextmenu.prevent="visible = false"
      >
        <div
          v-for="item in filteredItems"
          :key="item.key"
          class="context-menu__item"
          :class="{ 'context-menu__item--disabled': item.disabled }"
          @click="handleClick(item)"
        >
          <el-icon v-if="item.icon" :size="14" class="context-menu__icon">
            <component :is="item.icon" />
          </el-icon>
          <span class="context-menu__label">{{ item.label }}</span>
          <span v-if="item.shortcut" class="context-menu__shortcut">{{ item.shortcut }}</span>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import type { Component } from 'vue'

/**
 * ContextMenu - 右键菜单组件
 *
 * 用于表格行右键操作菜单，支持：
 * - 动态菜单项
 * - 权限控制（disabled）
 * - 快捷键提示
 * - 图标
 *
 * 用法：
 * ```vue
 * <ContextMenu ref="contextMenuRef" :items="menuItems" @select="handleSelect" />
 *
 * // 在表格上绑定
 * <el-table @row-contextmenu.prevent="showContextMenu">
 *
 * // 显示菜单
 * function showContextMenu(row, column, event) {
 *   contextMenuRef.value?.show(event, row)
 * }
 * ```
 */

export interface ContextMenuItem {
  /** 菜单项 key */
  key: string
  /** 显示标签 */
  label: string
  /** 图标组件 */
  icon?: Component
  /** 快捷键提示 */
  shortcut?: string
  /** 是否禁用 */
  disabled?: boolean
  /** 是否分割线 */
  divider?: boolean
}

const props = defineProps<{
  /** 菜单项列表 */
  items: ContextMenuItem[]
}>()

const emit = defineEmits<{
  select: [key: string, data: unknown]
}>()

const visible = ref(false)
const menuStyle = ref({})
const position = ref({ x: 0, y: 0 })
const menuData = ref<unknown>(null)

const filteredItems = computed(() => {
  return props.items.filter(item => !item.divider && !item.disabled)
})

const show = (event: MouseEvent, data?: unknown) => {
  event.preventDefault()
  event.stopPropagation()

  position.value = { x: event.clientX, y: event.clientY }
  menuData.value = data
  visible.value = true

  nextTick(() => {
    adjustPosition()
  })
}

const hide = () => {
  visible.value = false
}

const adjustPosition = () => {
  const menu = document.querySelector('.context-menu')
  if (!menu) return

  const rect = menu.getBoundingClientRect()
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight

  let x = position.value.x
  let y = position.value.y

  // 右侧溢出
  if (x + rect.width > viewportWidth) {
    x = viewportWidth - rect.width - 8
  }

  // 底部溢出
  if (y + rect.height > viewportHeight) {
    y = viewportHeight - rect.height - 8
  }

  menuStyle.value = {
    left: `${x}px`,
    top: `${y}px`,
  }
}

const handleClick = (item: ContextMenuItem) => {
  if (item.disabled) return
  emit('select', item.key, menuData.value)
  visible.value = false
}

// 点击外部关闭
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.context-menu')) {
    visible.value = false
  }
}

// ESC 关闭
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    visible.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ show, hide })
</script>

<style scoped>
.context-menu {
  position: fixed;
  z-index: 99999;
  min-width: 160px;
  background: var(--bg-container);
  border: 1px solid var(--border-light);
  border-radius: 6px;
  padding: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.context-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-primary);
  transition: background-color 0.1s;
}

.context-menu__item:hover {
  background-color: var(--bg-hover);
}

.context-menu__item--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.context-menu__item--disabled:hover {
  background-color: transparent;
}

.context-menu__icon {
  color: var(--text-secondary);
  flex-shrink: 0;
}

.context-menu__label {
  flex: 1;
}

.context-menu__shortcut {
  font-size: 11px;
  color: var(--text-placeholder);
  margin-left: 16px;
}

/* 动画 */
.context-menu-fade-enter-active,
.context-menu-fade-leave-active {
  transition: opacity 0.1s, transform 0.1s;
}

.context-menu-fade-enter-from,
.context-menu-fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>