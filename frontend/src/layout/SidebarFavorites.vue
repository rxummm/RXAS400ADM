<template>
  <div v-if="items.length" class="fav-panel">
    <div class="fav-title">
      <el-icon :size="12"><Star /></el-icon>
      <span>{{ $t('favorites.title') }}</span>
    </div>
    <div>
      <div
        v-for="item in items"
        :key="item.path"
        class="fav-item"
        :class="{ active: route.path === item.path }"
        :title="displayName(item)"
        @click="go(item)"
      >
        <el-icon v-if="iconInfo(item)?.kind === 'ep'" :size="14">
          <component :is="iconInfo(item)!.value" />
        </el-icon>
        <FontAwesomeIcon v-else-if="iconInfo(item)?.kind === 'fa'" :icon="faIconOr(item.icon)" class="fa-icon" />
        <span class="fav-name">{{ displayName(item) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Star } from '@element-plus/icons-vue'
import { listMyFavorites, type Favorite } from '@/api/favorite'
import { FontAwesomeIcon, resolveIcon, faIconOr } from '@/icons'

defineOptions({ name: 'SidebarFavorites' })

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const items = ref<Favorite[]>([])

const displayName = (item: Favorite) => {
  // 收藏存的是 i18n key（如 menu.jobs），能翻译则翻译
  const key = item.title
  if (key && key.startsWith('menu.') && t(key) !== key) {
    return t(key)
  }
  return key
}

const iconInfo = (item: Favorite) => resolveIcon(item.icon)

async function refresh() {
  try {
    items.value = (await listMyFavorites()) || []
  } catch {
    items.value = []
  }
}

function go(item: Favorite) {
  router.push(item.path)
}

onMounted(refresh)

defineExpose({ refresh })
</script>

<style scoped>
.fav-panel {
  padding: 4px 0 10px;
  border-top: 1px solid var(--sidebar-logo-border);
}
.fav-title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px 4px;
  font-size: 11px;
  color: var(--sidebar-text);
  opacity: 0.7;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}
.fav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 2px 8px;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  color: var(--sidebar-text);
  font-size: 13px;
  transition: all 0.15s;
}
.fav-item:hover {
  background: var(--sidebar-hover);
  color: var(--sidebar-text-active);
}
.fav-item.active {
  background: var(--color-primary);
  color: var(--bg-container);
}
.fav-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>