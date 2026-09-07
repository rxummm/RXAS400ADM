<template>
  <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
    <div class="logo" @click="router.push('/')">
      <el-icon :size="22"><Monitor /></el-icon>
      <span v-show="!isCollapse" class="logo-text">RXAS400</span>
    </div>
    <el-scrollbar>
      <el-menu
        router
        :default-active="route.path"
        :collapse="isCollapse"
        :collapse-transition="false"
        :unique-opened="true"
        class="menu"
      >
        <SubMenu v-for="menu in userStore.menus" :key="menu.path" :menu="menu" />
      </el-menu>
    </el-scrollbar>
    <SidebarFavorites v-if="!isCollapse" ref="favoritesRef" />
  </el-aside>
</template><script setup lang="ts">
import { ref } from 'vue'
import { Monitor } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import SubMenu from './SubMenu.vue'
import SidebarFavorites from './SidebarFavorites.vue'

defineProps<{
  isCollapse: boolean
}>()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const favoritesRef = ref<InstanceType<typeof SidebarFavorites>>()

defineExpose({ favoritesRef })
</script>

<style scoped>
.aside {
  display: flex;
  flex-direction: column;
  background: var(--sidebar-bg);
  transition: width 0.2s;
  overflow: hidden;
}
.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 56px;
  flex-shrink: 0;
  color: var(--sidebar-logo-color);
  cursor: pointer;
  border-bottom: 1px solid var(--sidebar-logo-border);
}
.logo-text {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
  white-space: nowrap;
  overflow: hidden;
}
.menu {
  border-right: none;
  --el-menu-icon-width: 20px;
  --el-menu-bg-color: var(--sidebar-bg);
  --el-menu-text-color: var(--sidebar-text);
  --el-menu-active-color: var(--sidebar-text-active);
  --el-menu-hover-bg-color: var(--sidebar-hover);
}
.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  margin: 2px 8px;
  border-radius: 4px;
  transition: all 0.15s;
}
.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) {
  background: var(--sidebar-hover);
}
.menu :deep(.el-menu-item.is-active) {
  background: var(--color-primary);
  color: var(--bg-container);
}
.menu :deep(.el-sub-menu .el-menu) {
  background-color: var(--sidebar-submenu-bg);
}
.menu.is-collapse :deep(.el-menu--popup) {
  --el-menu-bg-color: var(--sidebar-bg);
  --el-menu-text-color: var(--sidebar-text);
  --el-menu-hover-bg-color: var(--sidebar-hover);
}
</style>