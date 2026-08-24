<template>
  <div class="page-container page-container--fit">
    <div class="role-layout">
      <!-- ============ 左：角色列表 ============ -->
      <div class="role-list">
        <QueryBar
          v-model:keyword="keyword"
          :placeholder="$t('role.roleName') + ' / ' + $t('role.roleCode')"
          :keyword-width="180"
          :flash-tick="dataSourceTick"
          @force-search="handleRefresh"
          @reset="resetSearch"
        >
          <template #right>
            <div class="flex-1" />
            <el-button
              type="danger"
              plain
              :disabled="selectedIds.length === 0"
              @click="onBatchDelete"
            >
              <el-icon><Delete /></el-icon> {{ $t('common.batchDelete') }}
            </el-button>
            <ExportButton :data="displayRecords" :columns="exportColumns" :title="$t('role.roleName')" />
            <TableColumnSettings :columns="columnOptions" v-model:visible="visibleColumns" />
            <el-button type="primary" @click="openCreate()">
              <el-icon><Plus /></el-icon> {{ $t('role.addRole') }}
            </el-button>
          </template>
        </QueryBar>

        <div class="table-wrapper">
          <RxSkeleton type="table" :rows="8" :loading="loading">
            <el-table
              :data="displayRecords"
              border
              stripe
              size="small"
              :max-height="tableMaxHeight"
            highlight-current-row
            @selection-change="handleSelectionChange"
            @row-click="onRowClick"
          >
            <el-table-column type="selection" width="40" />
            <el-table-column
              v-if="visibleColumns.includes('roleName')"
              prop="roleName"
              :label="$t('role.roleName')"
              min-width="130"
            />
            <el-table-column
              v-if="visibleColumns.includes('roleCode')"
              prop="roleCode"
              :label="$t('role.roleCode')"
              width="120"
            />
            <el-table-column
              v-if="visibleColumns.includes('description')"
              prop="description"
              :label="$t('role.description')"
              min-width="150"
              show-overflow-tooltip
            />
            <el-table-column
              v-if="visibleColumns.includes('sort')"
              prop="sort"
              :label="$t('common.sort')"
              width="60"
              align="center"
            />
            <el-table-column v-if="visibleColumns.includes('status')" :label="$t('common.status')" width="80" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.status === 1"
                  :disabled="row.roleCode === 'ADMIN'"
                  size="small"
                  @change="(val: string | number | boolean) => onToggleStatus(row as SysRole, Boolean(val))"
                />
              </template>
            </el-table-column>
            <el-table-column :label="$t('common.operation')" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click.stop="openEdit(row as SysRole)">
                  {{ $t('common.edit') }}
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  :disabled="row.roleCode === 'ADMIN'"
                  @click.stop="onDelete(row as SysRole)"
                >
                  {{ $t('common.delete') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          </RxSkeleton>
          <AppPagination
            :total="total"
            v-model:size="size"
            show-sizes
            :page-sizes="pageSizes"
            v-model:current="current"
            @change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>

      <!-- ============ 右：菜单授权树 ============ -->
      <div class="role-perms">
        <div class="perms-header">
          <div class="perms-title">
            <span class="perms-label">{{ $t('role.menuPerms') }}</span>
            <span v-if="selectedRole" class="perms-role">{{ selectedRole.roleName }}</span>
            <span v-else class="perms-hint">{{ $t('role.selectRoleHint') }}</span>
          </div>
          <el-button
            type="primary"
            size="small"
            :disabled="!selectedRole"
            :loading="permSaving"
            @click="savePerms"
          >
            {{ $t('role.savePerms') }}
          </el-button>
        </div>
        <RxSkeleton type="card" :rows="6" :loading="menuLoading">
        <div class="menu-tree-wrap">
          <el-tree
            ref="menuTreeRef"
            :data="menuTree"
            show-checkbox
            node-key="id"
            default-expand-all
            :props="{ label: 'menuName', children: 'children' }"
          />
          <el-empty
            v-if="!menuLoading && menuTree.length === 0"
            :description="$t('role.noMenu')"
            :image-size="60"
          />
        </div>
      </RxSkeleton>
      </div>
    </div>

    <!-- 新建 / 编辑角色（基础字段，菜单授权在右侧面板） -->
    <RoleFormDialog v-model="roleDialogVisible" ref="roleFormRef" @saved="onRoleSaved" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import QueryBar from '@/components/QueryBar.vue'
import ExportButton from '@/components/ExportButton.vue'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import {
  fetchRolePage,
  updateRole,
  deleteRole,
  batchDeleteRoles,
  type SysRole,
} from '@/api/role'
import { getMenuTree, type SysMenu } from '@/api/menu'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import RoleFormDialog from './RoleFormDialog.vue'

defineOptions({ name: 'Roles' })

const { t } = useI18n()

const {
  records,
  loading,
  keyword,
  current,
  size,
  total,
  pageSizes,
  tableMaxHeight,
  dataSourceTick,
  fetchData,
  resetSearch,
  handleRefresh,
  handlePageChange,
  handleSizeChange,
} = useSmartQueryTable<SysRole>({
  fetchApi: (params) => fetchRolePage(params),
  defaultSize: 10,
  pageSizes: [10, 20, 50],
  enableResize: true,
})

// ==================== 列显隐配置 ====================
// 低-19：computed 惰性求值，切语言后列标题随响应式更新（原 setup 期一次性快照）
const columnOptions = computed(() => [
  { key: 'roleName', label: t('role.roleName') },
  { key: 'roleCode', label: t('role.roleCode') },
  { key: 'description', label: t('role.description') },
  { key: 'sort', label: t('common.sort') },
  { key: 'status', label: t('common.status') },
])
const visibleColumns = ref<string[]>(['roleName', 'roleCode', 'description', 'sort', 'status'])
const exportColumns = computed(() => columnOptions.value.map((c) => ({ key: c.key, label: c.label })))

// ==================== 多选 / 批量删除 ====================
const selectedIds = ref<number[]>([])

function handleSelectionChange(selection: SysRole[]) {
  selectedIds.value = selection.map((r) => r.id as number).filter(Boolean)
}

async function onBatchDelete() {
  try {
    await ElMessageBox.confirm(
      t('role.batchDeleteConfirm', { count: selectedIds.value.length }),
      t('common.tip'),
      { type: 'warning' },
    )
    await batchDeleteRoles(selectedIds.value)
    ElMessage.success(t('common.deleteSuccess'))
    selectedIds.value = []
    void fetchData({}, true)
  } catch {
    /* cancelled */
  }
}

// ==================== 右栏：菜单授权 ====================
const menuTree = ref<SysMenu[]>([])
const menuTreeRef = ref()
const menuLoading = ref(false)
const permSaving = ref(false)
const selectedRole = ref<SysRole | null>(null)

async function onRowClick(row: SysRole) {
  await selectRole(row)
}

async function selectRole(row: SysRole) {
  selectedRole.value = row
  menuLoading.value = true
  try {
    menuTree.value = (await getMenuTree()) || []
    await new Promise((r) => setTimeout(r))
    // 只回显叶子节点（父节点由树推导半选，防止级联权限膨胀，参照旧项目）
    const ids = (row.menuIds || []).filter((id) => {
      const node = menuTreeRef.value?.getNode(id)
      return node && (!node.childNodes || node.childNodes.length === 0)
    })
    menuTreeRef.value?.setCheckedKeys(ids || [])
  } finally {
    menuLoading.value = false
  }
}

async function savePerms() {
  if (!selectedRole.value?.id) return
  // 勾选叶子 + 半选父节点（父节点保证目录结构完整，参照旧项目）
  const checked = menuTreeRef.value?.getCheckedKeys() || []
  const halfChecked = menuTreeRef.value?.getHalfCheckedKeys() || []
  const menuIds = [...new Set<number>([...checked, ...halfChecked])]
  permSaving.value = true
  try {
    await updateRole(selectedRole.value.id, { ...selectedRole.value, menuIds })
    ElMessage.success(t('role.permsSaved'))
    void fetchData({}, true)
  } finally {
    permSaving.value = false
  }
}

// ==================== 新建 / 编辑 ====================
const roleFormRef = ref<InstanceType<typeof RoleFormDialog>>()
const roleDialogVisible = ref(false)

function openCreate() {
  roleFormRef.value?.openCreate()
}

async function openEdit(row: SysRole) {
  roleFormRef.value?.openEdit(row)
}

async function onRoleSaved() {
  void fetchData({}, true)
}

async function onToggleStatus(row: SysRole, enabled: boolean) {
  if (!row.id) return
  await updateRole(row.id, { ...row, status: enabled ? 1 : 0 })
  ElMessage.success(t('role.statusChanged'))
  void fetchData({}, true)
}

async function onDelete(row: SysRole) {
  try {
    await ElMessageBox.confirm(t('role.deleteConfirm', { name: row.roleName }), t('common.tip'), {
      type: 'warning',
    })
    if (row.id) await deleteRole(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    void fetchData({}, true)
  } catch {
    /* cancelled */
  }
}

// 表格渲染数据：前端关键字过滤（仅当前页内） + 高亮已选中
const displayRecords = computed(() => {
  if (!keyword.value.trim()) return records.value
  const kw = keyword.value.trim().toLowerCase()
  return records.value.filter(
    (r) =>
      (r.roleName || '').toLowerCase().includes(kw) ||
      (r.roleCode || '').toLowerCase().includes(kw) ||
      (r.description || '').toLowerCase().includes(kw),
  )
})

onMounted(() => {
  // 默认选中第一行便于直接授权
  void fetchData({}, true)
})
</script>

<style scoped>
.role-layout {
  display: flex;
  gap: 12px;
  height: 100%;
  min-height: 0;
}
.role-list {
  flex: 0 0 62%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.role-list .table-wrapper {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.role-perms {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-container);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 12px;
  overflow: hidden;
}
.perms-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
}
.perms-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.perms-label {
  font-weight: 600;
  font-size: 14px;
}
.perms-role {
  color: var(--color-primary);
  font-weight: 600;
}
.perms-hint {
  color: var(--text-secondary);
  font-size: 12px;
}
.menu-tree-wrap {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 4px;
}
</style>