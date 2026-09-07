<template>
  <el-container class="layout">
    <LayoutSidebar ref="sidebarRef" :is-collapse="isCollapse" />

    <el-container class="right">
      <el-header class="header">
        <div class="header-left">
          <el-tooltip :content="isCollapse ? $t('layout.expand') : $t('layout.collapse')" placement="bottom">
            <div class="header-action-btn" @click="isCollapse = !isCollapse">
              <el-icon :size="18"><Expand v-if="isCollapse" /><Fold v-else /></el-icon>
            </div>
          </el-tooltip>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">{{ $t('layout.home') }}</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">
              {{ $t(route.meta.title as string) }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <LayoutHeaderActions
          :is-fav="isFav"
          :is-dark="isDark"
          :is-fullscreen="isFullscreen"
          :current-theme="currentTheme"
          :theme-colors="[...themeColors]"
          :username="userStore.username"
          @toggle-fav="toggleFav"
          @toggle-dark="toggleDark"
          @theme-color="onThemeColor"
          @refresh="refreshCurrent"
          @toggle-fullscreen="toggleFullscreen"
          @open-command-palette="commandPaletteRef?.open()"
          @open-shortcuts="openShortcutsHelp"
          @toggle-locale="toggleLocale"
          @user-command="onCommand"
        />
      </el-header>

      <TagsView />

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <keep-alive :include="tagsStore.cachedViews">
            <transition name="fade-transform" mode="out-in">
              <component :is="Component" :key="route.path + (refreshKey || '')" />
            </transition>
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>

    <ShortcutsHelp ref="shortcutsHelpRef" />
    <ChangePasswordDialog ref="pwdDialogRef" />
    <CommandPalette ref="commandPaletteRef" />
    <AnnouncementPopup />
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { Expand, Fold } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useTagsStore } from '@/stores/tags'
import { setLocale } from '@/i18n'
import { loadDynamicI18n } from '@/i18n/dynamic'
import { useTheme, type ThemeColorName } from '@/composables/useTheme'
import { useShortcuts } from '@/composables/useShortcuts'
import LayoutSidebar from './LayoutSidebar.vue'
import TagsView from './TagsView.vue'
import LayoutHeaderActions from './LayoutHeaderActions.vue'
import ShortcutsHelp from '@/components/ShortcutsHelp.vue'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'
import CommandPalette from '@/components/CommandPalette.vue'
import AnnouncementPopup from '@/components/AnnouncementPopup.vue'
import { checkFavorite, toggleFavorite } from '@/api/favorite'
import { startTokenRefreshTimer, stopTokenRefreshTimer } from '@/composables/useTokenRefresh'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const tagsStore = useTagsStore()
const { locale, t } = useI18n()
const { isDark, toggleDark, currentTheme, setThemeColor, themeColors } = useTheme()

const isCollapse = ref(false)
const isFullscreen = ref(false)
const shortcutsHelpRef = ref<InstanceType<typeof ShortcutsHelp>>()
const pwdDialogRef = ref<InstanceType<typeof ChangePasswordDialog>>()
const commandPaletteRef = ref<InstanceType<typeof CommandPalette>>()
const sidebarRef = ref<InstanceType<typeof LayoutSidebar>>()
const isFav = ref(false)
const favLoading = ref(false)

const refreshFav = async () => {
  try {
    isFav.value = await checkFavorite(route.path)
  } catch {
    isFav.value = false
  }
}
const toggleFav = async () => {
  favLoading.value = true
  try {
    const res: { favorited: boolean } = await toggleFavorite({
      title: (route.meta.title as string) || route.path,
      path: route.path,
    })
    isFav.value = res.favorited
    ElMessage.success(t(res.favorited ? 'favorites.added' : 'favorites.removed'))
    sidebarRef.value?.favoritesRef?.refresh()
  } finally {
    favLoading.value = false
  }
}

const refreshKey = computed(() => tagsStore.refreshKeys[route.path] || '')

const refreshCurrent = () => {
  tagsStore.refreshView({ path: route.path, title: (route.meta.title as string) || '' })
  ElMessage.success(t('layout.refreshDone'))
}

const toggleLocale = async () => {
  const next = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  await loadDynamicI18n(next)
  setLocale(next)
}

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => {})
  } else {
    document.exitFullscreen().catch(() => {})
  }
}

const onThemeColor = (name: string) => {
  setThemeColor(name as ThemeColorName)
  ElMessage.success(t('layout.themeChanged'))
}

const openShortcutsHelp = () => {
  shortcutsHelpRef.value?.open(getRegisteredShortcuts())
}

const onCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    tagsStore.removeAllViews()
    router.push('/login')
  }
  if (command === 'profile') {
    pwdDialogRef.value?.open()
  }
  if (command === 'permissionRequest') {
    router.push('/system/permission-request')
  }
}

const { getRegisteredShortcuts } = useShortcuts([
  {
    key: 'Ctrl+B',
    description: t('layout.shortcutCollapse'),
    handler: () => (isCollapse.value = !isCollapse.value),
  },
  {
    key: 'Ctrl+D',
    description: t('layout.shortcutTheme'),
    handler: toggleDark,
  },
  {
    key: 'Ctrl+R',
    description: t('layout.shortcutRefresh'),
    handler: refreshCurrent,
  },
  {
    key: '?',
    description: t('layout.shortcutHelp'),
    handler: openShortcutsHelp,
  },
])

onMounted(() => {
  if (userStore.isLoggedIn && userStore.menus.length === 0) {
    void userStore.fetchMenus()
  }
  refreshFav()
  document.addEventListener('fullscreenchange', onFullscreenChange)
  // P2: 启动 token 过期前主动刷新定时器
  startTokenRefreshTimer()
})

watch(() => route.path, refreshFav)

const onFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
}

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  stopTokenRefreshTimer()
})
</script>

<style scoped>
.layout {
  height: 100vh;
  width: 100%;
}
.right {
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--header-bg);
  border-bottom: 1px solid var(--header-border);
  height: 56px;
  padding: 0 16px;
  flex-shrink: 0;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.header :deep(.el-breadcrumb) {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
}
.header :deep(.el-breadcrumb__inner) {
  color: var(--text-secondary);
  font-weight: 400;
  transition: color 0.15s;
}
.header :deep(.el-breadcrumb__inner:hover) {
  color: var(--color-primary);
}
.header :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--text-primary);
  font-weight: 500;
}
.main {
  flex: 1;
  min-height: 0;
  background: var(--bg-page);
  overflow: auto;
  padding: 12px;
}
</style>

<style>
/* 注：以下 header 操作区样式被 LayoutHeaderActions（子组件）使用，父组件 scoped 到不了子组件内部，故放全局块 */
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.header-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--text-regular);
  transition: all 0.15s;
}
.header-fa-icon {
  font-size: 17px;
  width: 17px;
  height: 17px;
}
.header-search-global {
  width: auto;
  gap: 4px;
  padding: 0 10px;
  font-size: 14px;
  color: var(--text-secondary);
  border: 1px solid var(--border-light);
  border-radius: 999px;
}
.header-search-global:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--bg-active);
}
.header-search-global kbd {
  font-size: 10px;
  padding: 1px 5px;
  line-height: 1.5;
  color: var(--text-secondary);
  background: var(--bg-hover);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  user-select: none;
}
.header-action-btn:hover {
  background: var(--bg-hover);
  color: var(--color-primary);
}
.header-action-btn:active {
  transform: scale(0.94);
}
.user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 8px;
  margin-left: 4px;
  border-radius: 6px;
  transition: background 0.15s;
}
.user:hover {
  background: var(--bg-hover);
}
.user-avatar {
  background: var(--color-primary);
  color: var(--bg-container);
}
.username {
  font-size: 14px;
  color: var(--text-primary);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.theme-color-dot {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  margin-right: 8px;
  vertical-align: middle;
  border: 1px solid var(--border-color);
}
.theme-active {
  font-weight: 600;
}
.theme-checked {
  margin-left: auto;
  font-size: 14px;
  color: var(--color-primary);
}
.header-action-btn.fav-active {
  color: var(--color-warning);
}
.header-action-btn.fav-active:hover {
  color: var(--color-warning);
}
.el-dropdown-menu .user-meta {
  color: var(--text-regular);
  font-weight: 600;
  cursor: default;
}
</style>