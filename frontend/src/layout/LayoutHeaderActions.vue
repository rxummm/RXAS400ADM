<template>
  <div class="header-right">
    <el-tooltip :content="isFav ? $t('favorites.remove') : $t('favorites.add')" placement="bottom">
      <div class="header-action-btn" :class="{ 'fav-active': isFav }" @click="$emit('toggleFav')">
        <el-icon :size="16"><StarFilled v-if="isFav" /><Star v-else /></el-icon>
      </div>
    </el-tooltip>
    <el-tooltip :content="isDark ? $t('layout.switchLight') : $t('layout.switchDark')" placement="bottom">
      <div class="header-action-btn" @click="$emit('toggleDark')">
        <el-icon :size="16"><Sunny v-if="isDark" /><Moon v-else /></el-icon>
      </div>
    </el-tooltip>
    <el-tooltip :content="$t('layout.themeColor')" placement="bottom">
      <el-dropdown trigger="click" @command="(name: string) => $emit('themeColor', name)">
        <div class="header-action-btn">
          <FontAwesomeIcon :icon="faIconOr('fa-solid fa-palette')" class="header-fa-icon" />
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-for="opt in themeColors" :key="opt.name" :command="opt.name">
              <span class="theme-color-dot" :style="{ background: opt.color }"></span>
              <span :class="{ 'theme-active': currentTheme === opt.name }">{{ $t(opt.label) }}</span>
              <el-icon v-if="currentTheme === opt.name" class="theme-checked"><Check /></el-icon>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-tooltip>
    <el-tooltip :content="$t('layout.refresh')" placement="bottom">
      <div class="header-action-btn" @click="$emit('refresh')">
        <el-icon :size="16"><Refresh /></el-icon>
      </div>
    </el-tooltip>
    <el-tooltip :content="isFullscreen ? $t('layout.exitFullscreen') : $t('layout.fullscreen')" placement="bottom">
      <div class="header-action-btn" @click="$emit('toggleFullscreen')">
        <FontAwesomeIcon
          :icon="faIconOr(isFullscreen ? 'fa-solid fa-compress' : 'fa-solid fa-expand')"
          class="header-fa-icon"
        />
      </div>
    </el-tooltip>
    <el-tooltip :content="`${$t('commandPalette.title')} (Ctrl+K)`" placement="bottom">
      <div class="header-action-btn header-search-global" @click="$emit('openCommandPalette')">
        <el-icon :size="15"><Search /></el-icon>
        <kbd>Ctrl+K</kbd>
      </div>
    </el-tooltip>
    <el-tooltip :content="$t('layout.shortcuts')" placement="bottom">
      <div class="header-action-btn" @click="$emit('openShortcuts')">
        <el-icon :size="16"><QuestionFilled /></el-icon>
      </div>
    </el-tooltip>
    <As400ServerSelector />
    <NotificationBell />
    <el-tooltip :content="$t('layout.switchLanguage')" placement="bottom">
      <div class="header-action-btn" @click="$emit('toggleLocale')">
        <FontAwesomeIcon :icon="faIconOr('fa-solid fa-globe')" class="header-fa-icon" />
      </div>
    </el-tooltip>
    <el-dropdown @command="(cmd: string) => $emit('userCommand', cmd)">
      <div class="user">
        <el-avatar :size="28" class="user-avatar">
          {{ username.charAt(0).toUpperCase() }}
        </el-avatar>
        <span class="username">{{ username }}</span>
        <el-icon :size="12"><ArrowDown /></el-icon>
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item disabled class="user-meta">
            <span>{{ username }}</span>
          </el-dropdown-item>
          <el-dropdown-item divided command="profile">
            <el-icon><User /></el-icon>
            {{ $t('layout.profile') }}
          </el-dropdown-item>
          <el-dropdown-item command="permissionRequest">
            <el-icon><Key /></el-icon>
            {{ $t('permissionRequest.title') }}
          </el-dropdown-item>
          <el-dropdown-item command="logout">
            <el-icon><SwitchButton /></el-icon>
            {{ $t('layout.logout') }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown, Check, Key, Moon, QuestionFilled, Refresh, Search, Star, StarFilled, Sunny, SwitchButton, User } from '@element-plus/icons-vue'
import { FontAwesomeIcon, faIconOr } from '@/icons'
import type { ThemeColorName } from '@/composables/useTheme'
import As400ServerSelector from './As400ServerSelector.vue'
import NotificationBell from './NotificationBell.vue'

defineProps<{
  isFav: boolean
  isDark: boolean
  isFullscreen: boolean
  currentTheme: string
  themeColors: { name: ThemeColorName; color: string; label: string }[]
  username: string
}>()

defineEmits<{
  toggleFav: []
  toggleDark: []
  themeColor: [name: string]
  refresh: []
  toggleFullscreen: []
  openCommandPalette: []
  openShortcuts: []
  toggleLocale: []
  userCommand: [command: string]
}>()
</script>