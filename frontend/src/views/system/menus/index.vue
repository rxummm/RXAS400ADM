<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        :placeholder="$t('menu.manage.keywordHint')"
        clearable
        class="w-240"
        @keyup.enter="applyKeyword"
      />
      <el-button type="primary" @click="applyKeyword">
        <el-icon><Search /></el-icon> {{ $t('common.search') }}
      </el-button>
      <el-button @click="resetSearch">{{ $t('common.reset') }}</el-button>
      <div class="flex-1" />
      <el-button type="primary" @click="openCreate()">
        <el-icon><Plus /></el-icon> {{ $t('menu.manage.addMenu') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
      <el-table
        :data="pagedTree"
        row-key="id"
        border
        stripe
        size="small"
        class="w-full"
      >
        <el-table-column prop="menuName" :label="$t('menu.manage.menuName')" width="160" show-overflow-tooltip />
        <el-table-column prop="icon" :label="$t('menu.manage.icon')" width="70" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.icon && !isFaIcon(row.icon) && hasIcon(row.icon)">
              <component :is="row.icon" />
            </el-icon>
            <FontAwesomeIcon v-else-if="row.icon && isFaIcon(row.icon)" :icon="faIconFor(row.icon)" />
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="menuType" :label="$t('menu.manage.type')" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.menuType)">{{ typeLabel(row.menuType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" :label="$t('menu.manage.path')" width="150" show-overflow-tooltip />
        <el-table-column :label="$t('menu.manage.perms')" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.menuType === MenuType.TAB && !row.perms" size="small" type="info">
              {{ $t('menuType.default') }}
            </el-tag>
            <el-tag v-else-if="row.perms" size="small" type="success">{{ row.perms }}</el-tag>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" :label="$t('common.sort')" width="70" align="center" />
        <el-table-column :label="$t('common.status')" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val: any) => onToggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" plain @click="openCreate(row)">
              <el-icon><Plus /></el-icon> {{ $t('menu.manage.addChild') }}
            </el-button>
            <el-button size="small" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button size="small" type="danger" @click="onDelete(row)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
    </div>

    <AppPagination :total="filteredTree.length" v-model:current="current" v-model:size="size" @size-change="onSizeChange" />

    <MenuFormDialog
      v-model="dialogVisible"
      :edit-data="editData"
      :parent-id="parentId"
      :menu-tree="menuTree"
      @saved="handleSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as Icons from '@element-plus/icons-vue'
import { FontAwesomeIcon, isFaIcon, faIconOr } from '@/icons'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import MenuFormDialog from './MenuFormDialog.vue'
import {
  getMenuTree,
  toggleMenuStatus,
  deleteMenu,
  MenuType,
  type SysMenu,
} from '@/api/menu'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'Menus' })

const { t } = useI18n()
const userStore = useUserStore()

const loading = ref(false)
const menuTree = ref<SysMenu[]>([])
const keyword = ref('')
const dialogVisible = ref(false)
const editData = ref<SysMenu | null>(null)
const parentId = ref<number | null>(null)

const hasIcon = (name: string) => !!(Icons as Record<string, unknown>)[name]
const faIconFor = (value?: string) => faIconOr(value)

const typeTag = (type: number) =>
  type === MenuType.DIR ? 'info' : type === MenuType.MENU ? 'success' : type === MenuType.BUTTON ? 'warning' : 'danger'
const typeLabel = (type: number) =>
  type === MenuType.DIR
    ? t('menu.manage.typeDir')
    : type === MenuType.MENU
      ? t('menu.manage.typeMenu')
      : type === MenuType.BUTTON
        ? t('menu.manage.typeButton')
        : t('menu.manage.typeTab')

const filteredTree = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return menuTree.value
  return filterTree(menuTree.value, kw)
})

const pagedTree = computed(() =>
  filteredTree.value.slice((current.value - 1) * size.value, current.value * size.value),
)

const current = ref(1)
const size = ref(15)

const onSizeChange = () => {
  current.value = 1
}

function filterTree(nodes: SysMenu[], kw: string): SysMenu[] {
  return nodes.reduce<SysMenu[]>((acc, node) => {
    const nameMatch = node.menuName?.toLowerCase().includes(kw)
    const permMatch = node.perms?.toLowerCase().includes(kw)
    const children = node.children ? filterTree(node.children, kw) : []
    if (nameMatch || permMatch || children.length > 0) {
      acc.push({ ...node, children })
    }
    return acc
  }, [])
}

const applyKeyword = () => {
  current.value = 1
}
const resetSearch = () => {
  keyword.value = ''
  current.value = 1
}

async function fetchData() {
  loading.value = true
  try {
    menuTree.value = await getMenuTree()
  } finally {
    loading.value = false
  }
}

function openCreate(parent?: SysMenu) {
  editData.value = null
  parentId.value = parent?.id ?? null
  dialogVisible.value = true
}

function openEdit(row: SysMenu) {
  editData.value = row
  parentId.value = null
  dialogVisible.value = true
}

async function handleSaved() {
  await fetchData()
  await userStore.fetchMenus()
}

async function onToggleStatus(row: SysMenu, enabled: boolean) {
  if (!row.id) return
  await toggleMenuStatus(row.id, enabled ? 1 : 0)
  ElMessage.success(t('menu.manage.statusChanged'))
  await fetchData()
  await userStore.fetchMenus()
}

async function onDelete(row: SysMenu) {
  try {
    await ElMessageBox.confirm(
      t('menu.manage.deleteConfirm', { name: row.menuName }),
      t('common.tip'),
      { type: 'warning' },
    )
    if (row.id) await deleteMenu(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    await fetchData()
    await userStore.fetchMenus()
  } catch {
    /* cancelled */
  }
}

onMounted(fetchData)
</script>