<template>
  <el-dialog
    v-model="visible"
    :title="t('users.permManage') + ' - ' + (user.username || '')"
    width="var(--rx-dialog-lg)"
    :close-on-click-modal="false"
    @opened="initPermManage"
    @closed="resetPermManage"
  >
    <el-tabs v-model="permManageTab" type="border-card">
      <!-- Tab1：当前权限（取消勾选 = 待移除） -->
      <el-tab-pane :label="t('users.currentPerms')" name="current">
        <el-alert type="info" :closable="false" show-icon class="mb16">
          <template #title>{{ t('users.currentPermsHint') }}</template>
        </el-alert>
        <el-scrollbar max-height="380px">
          <el-tree
            ref="currentPermTreeRef"
            :data="currentPermTree"
            show-checkbox
            node-key="id"
            default-expand-all
            :props="{ label: 'menuName', children: 'children' }"
            @check="handleCurrentPermCheck"
          >
            <template #default="{ data }">
              <span class="perm-node">
                <el-tag size="small" :type="menuTypeTag(data.menuType)">{{ menuTypeLabel(data.menuType) }}</el-tag>
                <span :class="{ 'pending-remove': data.id != null && pendingRemoveIds.has(data.id) }">{{ data.menuName }}</span>
                <el-tag v-if="data.menuType === MenuType.TAB && !data.perms" size="small" type="info" class="ml8">
                  {{ t('menuType.default') }}
                </el-tag>
                <span v-if="data.perms" class="perm-code">({{ data.perms }})</span>
              </span>
            </template>
          </el-tree>
        </el-scrollbar>
        <div class="perm-actions">
          <span class="text-muted">{{ t('users.pendingRemove', { count: pendingRemoveIds.size }) }}</span>
          <el-button
            type="danger"
            :disabled="pendingRemoveIds.size === 0"
            :loading="permManageLoading"
            @click="handleRemovePerms"
          >
            {{ t('users.removeSelected') }}
          </el-button>
        </div>
      </el-tab-pane>

      <!-- Tab2：可分配权限（勾选后分配） -->
      <el-tab-pane :label="t('users.assignPerms')" name="assign">
        <el-alert type="success" :closable="false" show-icon class="mb16">
          <template #title>{{ t('users.assignPermsHint') }}</template>
        </el-alert>
        <el-scrollbar max-height="380px">
          <el-tree
            ref="assignPermTreeRef"
            :data="assignPermTree"
            show-checkbox
            node-key="id"
            default-expand-all
            :props="{ label: 'menuName', children: 'children' }"
          >
            <template #default="{ data }">
              <span class="perm-node">
                <el-tag size="small" :type="menuTypeTag(data.menuType)">{{ menuTypeLabel(data.menuType) }}</el-tag>
                <span>{{ data.menuName }}</span>
                <el-tag v-if="data.menuType === MenuType.TAB && !data.perms" size="small" type="info" class="ml8">
                  {{ t('menuType.default') }}
                </el-tag>
                <span v-if="data.perms" class="perm-code">({{ data.perms }})</span>
              </span>
            </template>
          </el-tree>
        </el-scrollbar>
        <div class="perm-actions">
          <el-button
            type="primary"
            :disabled="getCheckedAssignKeys().length === 0"
            :loading="permManageLoading"
            @click="handleAddPerms"
          >
            {{ t('users.assignSelected') }}
          </el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  addUserMenus,
  getUserDirectMenuIds,
  getUserManageableTree,
  getUserMenuIds,
  removeUserMenus,
} from '@/api/user'
import type { SysMenu } from '@/api/menu'
import { MenuType } from '@/api/menu'

const props = defineProps<{
  modelValue: boolean
  user: { id: number; username: string }
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'updated': []
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const permManageTab = ref('current')
const permManageLoading = ref(false)
const currentPermTreeRef = ref()
const assignPermTreeRef = ref()
const currentPermTree = ref<SysMenu[]>([])
const assignPermTree = ref<SysMenu[]>([])
const originalPermIds = ref<Set<number>>(new Set())
const pendingRemoveIds = ref<Set<number>>(new Set())

const menuTypeTag = (type?: number) =>
  type === MenuType.DIR ? 'info' : type === MenuType.MENU ? 'success' : type === MenuType.BUTTON ? 'warning' : 'danger'
const menuTypeLabel = (type?: number) =>
  type === MenuType.DIR
    ? t('menuType.dir')
    : type === MenuType.MENU
      ? t('menuType.menu')
      : type === MenuType.BUTTON
        ? t('menuType.button')
        : t('menuType.tab')

function resetPermManage() {
  originalPermIds.value = new Set()
  pendingRemoveIds.value = new Set()
}

async function initPermManage() {
  await refreshPermManageData('current')
}

async function refreshPermManageData(keepTab?: string) {
  if (keepTab) permManageTab.value = keepTab
  permManageLoading.value = true
  try {
    const userId = props.user.id
    const [menuIds, directIds, fullTree] = await Promise.all([
      getUserMenuIds(userId),
      getUserDirectMenuIds(userId),
      getUserManageableTree(userId),
    ])
    const ownedSet = new Set<number>(menuIds || [])

    const ownedOnly = (nodes: SysMenu[]): SysMenu[] => {
      const result: SysMenu[] = []
      for (const n of nodes || []) {
        const children = n.children ? ownedOnly(n.children) : []
        if ((n.id != null && ownedSet.has(n.id)) || children.length > 0) {
          result.push({ ...n, children: children.length ? children : [] })
        }
      }
      return result
    }
    currentPermTree.value = ownedOnly(fullTree)
    originalPermIds.value = new Set(directIds || [])
    pendingRemoveIds.value = new Set()
    assignPermTree.value = fullTree || []

    await nextTick()
    if (currentPermTreeRef.value) {
      currentPermTreeRef.value.setCheckedKeys([...ownedSet])
    }
  } finally {
    permManageLoading.value = false
  }
}

function getCheckedAssignKeys() {
  return assignPermTreeRef.value?.getCheckedKeys() || []
}

interface TreeCheckInfo {
  checkedKeys: Array<number | string>
}

function handleCurrentPermCheck(_node: unknown, info: TreeCheckInfo) {
  const currentChecked = new Set<number>(info.checkedKeys as number[])
  const toRemove = new Set<number>()
  for (const id of originalPermIds.value) {
    if (!currentChecked.has(id)) toRemove.add(id)
  }
  pendingRemoveIds.value = toRemove
}

async function handleRemovePerms() {
  const removeIds = [...pendingRemoveIds.value]
  if (removeIds.length === 0) return
  try {
    await ElMessageBox.confirm(t('users.removeConfirm', { count: removeIds.length }), t('common.tip'), {
      type: 'warning',
    })
  } catch {
    return
  }
  permManageLoading.value = true
  try {
    await removeUserMenus(props.user.id, removeIds)
    ElMessage.success(t('common.deleteSuccess'))
    await refreshPermManageData(permManageTab.value)
    emit('updated')
  } catch {
    /* interceptor */
  } finally {
    permManageLoading.value = false
  }
}

async function handleAddPerms() {
  const keys = getCheckedAssignKeys()
  if (keys.length === 0) return
  try {
    await ElMessageBox.confirm(t('users.addConfirm'), t('common.tip'), { type: 'warning' })
  } catch {
    return
  }
  permManageLoading.value = true
  try {
    await addUserMenus(props.user.id, keys)
    ElMessage.success(t('users.addSuccess'))
    await refreshPermManageData(permManageTab.value)
    emit('updated')
  } catch {
    /* interceptor */
  } finally {
    permManageLoading.value = false
  }
}
</script>

<style scoped>
/* 权限树节点：类型标签 + 名称 + 权限码 横向排布 */
.perm-node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}
.perm-code {
  color: var(--text-secondary);
  font-size: 12px;
}
/* 底部操作行（待移除计数 + 按钮） */
.perm-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}
/* 勾选待移除的权限：删除线 + 危险色 */
.pending-remove {
  color: var(--color-danger);
  text-decoration: line-through;
}
</style>