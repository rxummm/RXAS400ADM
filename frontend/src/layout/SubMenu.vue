<template>
  <el-sub-menu v-if="menu.children && menu.children.length" :index="menu.path">
    <template #title>
      <el-icon v-if="iconInfo?.kind === 'ep'"><component :is="iconInfo.value" /></el-icon>
      <FontAwesomeIcon v-else-if="iconInfo?.kind === 'fa'" :icon="iconInfo.value" class="fa-icon" />
      <el-icon v-else><Menu /></el-icon>
      <span>{{ $t(`menu.${menu.title}`) }}</span>
    </template>
    <SubMenu v-for="child in menu.children" :key="child.path" :menu="child" />
  </el-sub-menu>
  <el-menu-item v-else :index="menu.path">
    <el-icon v-if="iconInfo?.kind === 'ep'"><component :is="iconInfo.value" /></el-icon>
    <FontAwesomeIcon v-else-if="iconInfo?.kind === 'fa'" :icon="iconInfo.value" class="fa-icon" />
    <el-icon v-else><Menu /></el-icon>
    <span>{{ $t(`menu.${menu.title}`) }}</span>
  </el-menu-item>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Menu } from '@element-plus/icons-vue'
import { resolveIcon } from '@/icons'
import type { MenuItem } from '@/stores/user'

defineOptions({ name: 'SubMenu' })
const props = defineProps<{ menu: MenuItem }>()

const iconInfo = computed(() => resolveIcon(props.menu.icon))
</script>
