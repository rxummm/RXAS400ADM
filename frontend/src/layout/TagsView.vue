<template>
  <div v-if="visitedViews.length" class="tags-view">
    <el-scrollbar class="tags-scrollbar" @wheel.prevent="handleWheel">
      <div ref="tagsWrapperRef" class="tags-wrapper">
        <router-link
          v-for="tag in visitedViews"
          :key="tag.path"
          :to="tag.path"
          class="tag-item"
          :class="{ active: isActive(tag.path), 'flash-pop': flashingPath === tag.path }"
          @click="handleTagClick($event, tag)"
          @contextmenu.prevent="openContextMenu($event, tag)"
        >
          <span class="tag-title">{{ $t(tag.title || '') }}</span>
          <el-icon
            v-if="!isAffix(tag)"
            class="tag-close"
            @click.prevent.stop="handleClose(tag)"
          >
            <Close />
          </el-icon>
        </router-link>
      </div>
    </el-scrollbar>

    <!-- 右键菜单 -->
    <Teleport to="body">
      <ul
        v-show="contextMenuVisible"
        class="tags-context-menu"
        :style="{ left: contextMenuLeft + 'px', top: contextMenuTop + 'px' }"
        @click="closeContextMenu"
      >
        <li @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          {{ $t('layout.tags.refresh') }}
        </li>
        <li :class="{ disabled: isAffix(selectedTag) }" @click="handleCloseCurrent">
          <el-icon><Close /></el-icon>
          {{ $t('layout.tags.closeCurrent') }}
        </li>
        <li @click="handleCloseOthers">
          <el-icon><CircleClose /></el-icon>
          {{ $t('layout.tags.closeOthers') }}
        </li>
        <li @click="handleCloseAll">
          <el-icon><Remove /></el-icon>
          {{ $t('layout.tags.closeAll') }}
        </li>
      </ul>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { storeToRefs } from 'pinia'
import { CircleClose, Close, Refresh, Remove } from '@element-plus/icons-vue'
import { useTagsStore, type TagView } from '@/stores/tags'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const tagsStore = useTagsStore()

const { visitedViews } = storeToRefs(tagsStore)
const tagsWrapperRef = ref<HTMLElement | null>(null)

/** 标签刷新闪烁：刷新某个标签时该标签短暂高亮（数据已更新反馈），1.4s 后熄灭 */
const flashingPath = ref('')
let flashTimer: number | undefined
const flashTag = (path: string) => {
  flashingPath.value = path
  if (flashTimer) window.clearTimeout(flashTimer)
  flashTimer = window.setTimeout(() => {
    flashingPath.value = ''
  }, 1400)
}

const contextMenuVisible = ref(false)
const contextMenuLeft = ref(0)
const contextMenuTop = ref(0)
const selectedTag = ref<TagView>({ path: '', title: '' })

const isActive = (path: string) => route.path === path
const isAffix = (tag: TagView) => !!tag.affix

const addTag = () => {
  const { path, meta } = route
  const title = (meta.title as string) || ''
  if (!title) return
  // cacheName：仅在路由声明 cached 时启用 keep-alive 缓存（组件 name 与路由 name 对齐）
  const cacheName = meta.cached ? (meta.cacheName as string) || (route.name as string) || undefined : undefined
  tagsStore.addView({ path, title, affix: path === '/dashboard', cacheName })
}

// ==================== 右键菜单 ====================
const openContextMenu = (e: MouseEvent, tag: TagView) => {
  contextMenuVisible.value = true
  contextMenuLeft.value = e.clientX
  contextMenuTop.value = e.clientY
  selectedTag.value = tag
}

const closeContextMenu = () => {
  contextMenuVisible.value = false
}

const handleRefresh = () => {
  flashTag(selectedTag.value.path)
  tagsStore.refreshView(selectedTag.value)
  ElMessage.success(t('layout.tags.refreshDone'))
  closeContextMenu()
}

const handleCloseCurrent = () => {
  if (isAffix(selectedTag.value)) {
    ElMessage.warning(t('layout.tags.affixWarning'))
    closeContextMenu()
    return
  }
  const nextPath = tagsStore.closeView(selectedTag.value)
  if (nextPath && isActive(selectedTag.value.path)) {
    router.push(nextPath)
  }
  closeContextMenu()
}

const handleCloseOthers = () => {
  tagsStore.removeOtherViews(selectedTag.value)
  closeContextMenu()
}

const handleCloseAll = async () => {
  tagsStore.removeAllViews()
  if (!visitedViews.value.some((v) => v.path === route.path)) {
    const target = visitedViews.value[0]?.path || '/dashboard'
    if (target === route.path) {
      // 关闭全部后回落到当前页（如总览）：原地刷新而不是无操作
      flashTag(target)
      tagsStore.refreshView({ path: target, title: (route.meta.title as string) || '' })
    } else {
      await router.push(target)
    }
  }
  closeContextMenu()
}

/** 点击标签：已激活的标签（如总览）触发刷新而不是无操作；其余交给 router-link 导航 */
const handleTagClick = (e: MouseEvent, tag: TagView) => {
  if (isActive(tag.path)) {
    e.preventDefault()
    flashTag(tag.path)
    tagsStore.refreshView(tag)
  }
}

const handleClose = (tag: TagView) => {
  if (isAffix(tag)) return
  const nextPath = tagsStore.closeView(tag)
  if (nextPath && isActive(tag.path)) {
    router.push(nextPath)
  }
}

const handleClickOutside = (e: MouseEvent) => {
  if (!(e.target as HTMLElement).closest('.tags-context-menu')) {
    closeContextMenu()
  }
}

// ==================== 滚动 ====================
const getScrollWrap = () =>
  tagsWrapperRef.value?.closest('.el-scrollbar')?.querySelector('.el-scrollbar__wrap') || null

const handleWheel = (e: WheelEvent) => {
  const wrap = getScrollWrap()
  if (wrap) {
    wrap.scrollLeft += e.deltaY + (e.deltaX || 0)
  }
}

const scrollToActiveTag = () => {
  const wrap = getScrollWrap()
  if (!wrap) return
  const activeEl = tagsWrapperRef.value?.querySelector('.tag-item.active') as HTMLElement | null
  if (!activeEl) return
  const wrapRect = wrap.getBoundingClientRect()
  const elRect = activeEl.getBoundingClientRect()
  if (elRect.left < wrapRect.left || elRect.right > wrapRect.right) {
    const target =
      wrap.scrollLeft + (elRect.left - wrapRect.left) - (wrapRect.width - activeEl.offsetWidth) / 2
    wrap.scrollTo({ left: Math.max(0, target), behavior: 'smooth' })
  }
}

watch(
  () => [route.path, visitedViews.value.length],
  () => nextTick(() => scrollToActiveTag()),
)

// 路由变化时添加标签（此前只在挂载时 addTag，导致切换页面不产生新标签、keep-alive 缓存名单不更新）
watch(() => route.path, addTag)

onMounted(() => {
  addTag()
  document.addEventListener('click', handleClickOutside)
  nextTick(() => scrollToActiveTag())
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (flashTimer) window.clearTimeout(flashTimer)
})
</script>

<style scoped>
.tags-view {
  flex-shrink: 0;
  background: var(--header-bg);
  border-bottom: 1px solid var(--header-border);
  user-select: none;
}
.tags-scrollbar {
  height: 36px;
  white-space: nowrap;
}
.tags-wrapper {
  display: inline-flex;
  align-items: center;
  height: 36px;
  padding: 0 8px;
  gap: 4px;
}
.tag-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 8px;
  font-size: 12px;
  color: var(--text-regular);
  background: var(--bg-container);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  cursor: pointer;
  text-decoration: none;
  white-space: nowrap;
  transition: all 0.15s;
}
.tag-item:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}
.tag-item.active {
  color: var(--bg-container);
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.tag-title {
  margin-right: 4px;
}
.tag-close {
  font-size: 12px;
  border-radius: 50%;
  padding: 1px;
}
.tag-close:hover {
  background: var(--bg-hover);
}
.tag-item.active .tag-close:hover {
  background: color-mix(in srgb, var(--text-primary) 25%, transparent);
}
</style>

<style>
/* 右键菜单（不 scoped，Teleport 到 body） */
.tags-context-menu {
  position: fixed;
  z-index: var(--z-context-menu, 3000);
  min-width: 140px;
  margin: 0;
  padding: 4px;
  list-style: none;
  background: var(--bg-container);
  border: 1px solid var(--border-light);
  border-radius: 6px;
  box-shadow: var(--shadow-lg);
}
.tags-context-menu li {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  font-size: 13px;
  color: var(--text-regular);
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.15s;
}
.tags-context-menu li:hover {
  background: var(--bg-hover);
  color: var(--color-primary);
}
.tags-context-menu li.disabled {
  color: var(--text-placeholder);
  cursor: not-allowed;
}
.tags-context-menu li.disabled:hover {
  background: transparent;
  color: var(--text-placeholder);
}
</style>