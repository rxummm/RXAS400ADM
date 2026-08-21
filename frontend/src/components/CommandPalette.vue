<template>
  <el-dialog
    v-model="visible"
    :show-close="false"
    width="560px"
    top="15vh"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    @opened="onOpened"
  >
    <div class="command-palette">
      <div class="cp-input-wrapper">
        <el-icon :size="18"><Search /></el-icon>
        <input
          ref="inputRef"
          v-model="keyword"
          :placeholder="$t('commandPalette.placeholder')"
          class="cp-input"
          @keydown="handleKeydown"
          @input="handleInput"
        />
        <el-tag size="small" class="cp-esc" @click="close">Esc</el-tag>
      </div>

      <div v-if="keyword" class="cp-results">
        <div v-if="menuResults.length" class="cp-group">
          <div class="cp-group-title">{{ $t('commandPalette.menus') }}</div>
          <div
            v-for="(item, i) in menuResults"
            :key="'menu-' + i"
            :class="['cp-item', { active: i === activeIndex }]"
            @click="goMenu(item)"
            @mouseenter="activeIndex = i"
          >
            <el-icon><Menu as="span" /></el-icon>
            <span>{{ item.name }}</span>
            <span class="cp-path">{{ item.path }}</span>
          </div>
        </div>

        <div v-if="recentResults.length" class="cp-group">
          <div class="cp-group-title">{{ $t('commandPalette.recent') }}</div>
          <div
            v-for="(item, i) in recentResults"
            :key="'recent-' + i"
            :class="['cp-item', { active: i + menuResults.length === activeIndex }]"
            @click="goMenu(item)"
            @mouseenter="activeIndex = i + menuResults.length"
          >
            <el-icon><Clock /></el-icon>
            <span>{{ item.name }}</span>
            <span class="cp-path">{{ item.path }}</span>
          </div>
        </div>

        <div v-if="userResults.length" class="cp-group">
          <div class="cp-group-title">{{ $t('commandPalette.users') }}</div>
          <div
            v-for="(item, i) in userResults"
            :key="'user-' + i"
            :class="['cp-item', { active: i + menuResults.length + recentResults.length === activeIndex }]"
            @click="goUsers"
            @mouseenter="activeIndex = i + menuResults.length + recentResults.length"
          >
            <el-icon><User /></el-icon>
            <span>{{ item.name }}</span>
            <span class="cp-path">{{ $t('commandPalette.gotoUsers') }}</span>
          </div>
        </div>

        <div v-if="actionResults.length" class="cp-group">
          <div class="cp-group-title">{{ $t('commandPalette.actions') }}</div>
          <div
            v-for="(item, i) in actionResults"
            :key="'action-' + i"
            :class="['cp-item', { active: i + menuResults.length + recentResults.length + userResults.length === activeIndex }]"
            @click="doAction(item)"
            @mouseenter="activeIndex = i + menuResults.length + recentResults.length + userResults.length"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.name }}</span>
          </div>
        </div>

        <div
          v-if="!menuResults.length && !recentResults.length && !userResults.length && !actionResults.length"
          class="cp-empty"
        >
          {{ $t('commandPalette.noResult') }}
        </div>
      </div>

      <div v-else class="cp-placeholder">
        <div class="cp-placeholder-icon"><el-icon :size="40"><Search /></el-icon></div>
        <div>{{ $t('commandPalette.hint') }}</div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Clock, FullScreen, HomeFilled, Menu, Search, SwitchButton, User } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useUserStore, type MenuItem } from '@/stores/user'
import { useTagsStore } from '@/stores/tags'
import { useTheme } from '@/composables/useTheme'
import { fetchUsers } from '@/api/user'

defineOptions({ name: 'CommandPalette' })

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const tagsStore = useTagsStore()
const { toggleDark } = useTheme()

const visible = ref(false)
const keyword = ref('')
const activeIndex = ref(0)
const inputRef = ref<HTMLInputElement>()
const userSearchResults = ref<{ name: string }[]>([])
const searchingUsers = ref(false)

// M3：searchTimer 为历史死代码（从未赋值，实际防抖走 userTimer），已删除
let userTimer: number | undefined

// 本地菜单（扁平化，含分组路径前缀）
// i18n key（menu.xxx）→ 文案，取不到则原样返回
const menuLabel = (key: string) => {
  const k = key.startsWith('menu.') ? key : `menu.${key}`
  const v = t(k)
  return v === k ? key : v
}

const flatMenus = computed(() => {
  const result: { name: string; path: string }[] = []
  const walk = (items: MenuItem[], parentName = '') => {
    for (const m of items || []) {
      const label = menuLabel(m.title)
      const fullName = parentName ? `${parentName} / ${label}` : label
      if (m.children?.length) {
        walk(m.children, fullName)
      } else if (m.path) {
        result.push({ name: fullName, path: m.path })
      }
    }
  }
  walk(userStore.menus)
  return result
})

const menuResults = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return []
  return flatMenus.value.filter((m) => m.name.toLowerCase().includes(kw)).slice(0, 12)
})

const recentResults = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return []
  return tagsStore.visitedViews
    .filter((v) => v.path && menuLabel(v.title || '').toLowerCase().includes(kw))
    .slice(0, 3)
    .map((v) => ({ name: menuLabel(v.title || ''), path: v.path }))
})

const actions = [
  { name: t('commandPalette.toggleTheme'), icon: FullScreen, action: () => toggleDark() },
  {
    name: t('commandPalette.toggleFullscreen'),
    icon: FullScreen,
    action: () => {
      if (document.fullscreenElement) document.exitFullscreen().catch(() => {})
      else document.documentElement.requestFullscreen().catch(() => {})
    },
  },
  { name: t('commandPalette.goHome'), icon: HomeFilled, action: () => router.push('/') },
  {
    name: t('commandPalette.logout'),
    icon: SwitchButton,
    action: () => {
      userStore.logout()
      tagsStore.removeAllViews()
      router.push('/login')
    },
  },
]

const actionResults = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return []
  return actions.filter((a) => a.name.toLowerCase().includes(kw))
})

const userResults = computed(() => {
  // 无用户管理权限时不展示用户搜索
  if (!userStore.hasPermission('USER_MANAGE')) return []
  return userSearchResults.value
})

const allResults = computed(() => {
  const list: { name: string; path?: string; go: () => void }[] = []
  if (keyword.value.trim()) {
    list.push(...menuResults.value.map((m) => ({ ...m, go: () => goMenu(m) })))
    list.push(...recentResults.value.map((m) => ({ ...m, go: () => goMenu(m) })))
    list.push(...userResults.value.map((m) => ({ ...m, go: goUsers })))
    list.push(...actionResults.value.map((m) => ({ ...m, go: () => doAction(m) })))
  }
  return list
})

function handleInput() {
  activeIndex.value = 0
  const kw = keyword.value.trim()
  if (!kw) {
    userSearchResults.value = []
    return
  }
  // 用户搜索（防抖 + 权限）
  if (userStore.hasPermission('USER_MANAGE')) {
    clearTimeout(userTimer)
    userTimer = window.setTimeout(async () => {
      searchingUsers.value = true
      try {
        const data: { records?: { username: string }[] } = await fetchUsers({ current: 1, size: 5, keyword: kw })
        userSearchResults.value = (data?.records || []).map((u: { username: string }) => ({ name: u.username }))
      } catch {
        userSearchResults.value = []
      } finally {
        searchingUsers.value = false
      }
    }, 250)
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeIndex.value = Math.min(activeIndex.value + 1, allResults.value.length - 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    activeIndex.value = Math.max(activeIndex.value - 1, 0)
  } else if (e.key === 'Enter') {
    e.preventDefault()
    const item = allResults.value[activeIndex.value]
    if (item?.go) item.go()
  }
}

function goMenu(item: { path: string }) {
  visible.value = false
  keyword.value = ''
  router.push(item.path)
}

function goUsers() {
  visible.value = false
  keyword.value = ''
  router.push('/users')
}

function doAction(item: { action: () => void }) {
  visible.value = false
  keyword.value = ''
  item.action()
}

function open() {
  visible.value = true
  keyword.value = ''
  activeIndex.value = 0
  userSearchResults.value = []
}

function onOpened() {
  nextTick(() => inputRef.value?.focus())
}

function close() {
  visible.value = false
  keyword.value = ''
}

function onGlobalKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    open()
  }
}

onMounted(() => document.addEventListener('keydown', onGlobalKeydown))
onUnmounted(() => {
  document.removeEventListener('keydown', onGlobalKeydown)
  // M3：原 searchTimer 为死代码，无清理必要
  if (userTimer) clearTimeout(userTimer)
})

defineExpose({ open, close })
</script>

<style scoped>
.command-palette :deep(.el-dialog__header) {
  display: none;
  padding: 0;
}
.command-palette :deep(.el-dialog__body) {
  padding: 0;
}
.cp-input-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
}
.cp-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  background: transparent;
  color: var(--el-text-color-primary);
}
.cp-input::placeholder {
  color: var(--text-placeholder);
}
.cp-esc {
  cursor: pointer;
  flex-shrink: 0;
}
.cp-results {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px 0;
}
.cp-empty,
.cp-placeholder {
  padding: 40px 0;
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
}
.cp-placeholder-icon {
  color: var(--text-placeholder);
  margin-bottom: 8px;
}
.cp-group {
  padding: 2px 0;
}
.cp-group + .cp-group {
  border-top: 1px solid var(--border-lighter);
}
.cp-group-title {
  padding: 4px 16px;
  font-size: 11px;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.cp-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.1s;
  color: var(--text-primary);
}
.cp-item:hover,
.cp-item.active {
  background: var(--bg-active);
}
.cp-item .cp-path {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-placeholder);
}
</style>