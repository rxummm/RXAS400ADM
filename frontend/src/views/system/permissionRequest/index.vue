<template>
  <div class="page-container page-container--fit">
    <el-tabs v-model="tab" class="page-tabs" @tab-click="onTabClick">
      <!-- ===== 我的申请（两列：左菜单树 + 右侧已选/提交/记录） ===== -->
      <el-tab-pane :label="$t('permissionRequest.myTab')" name="mine">
        <el-row :gutter="16">
          <!-- 左：可申请菜单树 -->
          <el-col :xs="24" :sm="24" :md="14" :span="14">
            <div class="table-wrapper">
              <div class="search-bar">
                <span class="section-title">{{ $t('permissionRequest.selectMenus') }}</span>
                <el-tag size="small" type="info">{{ checkedIds.length }} / {{ allMenuIds.length }}</el-tag>
                <div class="flex-1" />
                <el-button size="small" @click="expandAll">{{ $t('permissionRequest.expandAll') }}</el-button>
                <el-button size="small" @click="collapseAll">{{ $t('permissionRequest.collapseAll') }}</el-button>
              </div>
              <el-scrollbar max-height="560px">
                <el-tree
                  ref="treeRef"
                  :data="menuTree"
                  show-checkbox
                  node-key="id"
                  :props="{ label: 'menuName', children: 'children' }"
                  :default-expanded-keys="expandedKeys"
                  @check="handleTreeCheck"
                >
                  <template #default="{ data }: { data: RequestableMenu }">
                    <span class="tree-node">
                      <el-icon v-if="data.icon && !data.icon.startsWith('fa-')" class="mr6">
                        <component :is="data.icon" />
                      </el-icon>
                      <span>{{ data.menuName }}</span>
                      <el-tag size="small" :type="menuTypeTag(data.menuType)" class="ml8">
                        {{ menuTypeLabel(data.menuType) }}
                      </el-tag>
                      <el-tag v-if="data.menuType === MenuType.TAB && !data.perms" size="small" type="info" class="ml8">
                        {{ t('menuType.default') }}
                      </el-tag>
                      <span v-if="data.perms" class="tree-perms">({{ data.perms }})</span>
                    </span>
                  </template>
                </el-tree>
              </el-scrollbar>
            </div>
          </el-col>

          <!-- 右：已选 + 提交 + 我的申请 -->
          <el-col :xs="24" :sm="24" :md="10" :span="10">
            <div class="table-wrapper">
              <div class="search-bar">
                <span class="section-title">{{ $t('permissionRequest.selectedMenus') }}</span>
                <el-button
                  type="primary"
                  size="small"
                  :icon="Upload"
                  :loading="submitLoading"
                  :disabled="checkedIds.length === 0"
                  @click="handleSubmit"
                >
                  {{ $t('permissionRequest.submitApply') }}
                </el-button>
              </div>
              <el-scrollbar max-height="200px" class="selected-scroll">
                <div v-if="checkedIds.length === 0" class="empty-hint">
                  {{ $t('permissionRequest.noSelectHint') }}
                </div>
                <el-tag
                  v-for="menu in checkedTags"
                  :key="menu.id"
                  closable
                  size="small"
                  :type="menuTypeTag(menu.menuType)"
                  class="mr4 mb8"
                  @close="removeMenu(menu)"
                >
                  {{ menu.menuName }}
                </el-tag>
              </el-scrollbar>
            </div>

            <div class="table-wrapper mt16">
              <div class="search-bar">
                <span class="section-title">{{ $t('permissionRequest.myRequests') }}</span>
              </div>
              <el-scrollbar max-height="360px">
                <div v-if="mineRows.length === 0" class="empty-hint">{{ $t('permissionRequest.noRequest') }}</div>
                <div v-for="req in mineRows" :key="req.id" class="request-item">
                  <div class="request-header">
                    <span class="request-menus">{{ displayMenus(req) }}</span>
                    <el-tag size="small" :type="statusTag(req.status)">{{ statusLabel(req.status) }}</el-tag>
                  </div>
                  <div class="request-time">{{ formatTime(req.createdTime) }}</div>
                  <div v-if="req.reason" class="request-remark">{{ $t('permissionRequest.reason') }}：{{ req.reason }}</div>
                  <div v-if="req.approveComment" class="request-remark">
                    {{ $t('permissionRequest.approveComment') }}：{{ req.approveComment }}
                  </div>
                </div>
              </el-scrollbar>
              <AppPagination
                :total="mineTotal"
                v-model:current="mineCurrent"
                v-model:size="mineSize"
                @change="loadMine"
                @size-change="loadMine"
              />
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- ===== 审批（管理端：仅 SYS_PERMISSION_REQUEST 权限可见） ===== -->
      <el-tab-pane
        v-if="userStore.canSeeTab('permissionRequest', 'permissionRequestReview')"
        :label="`${$t('permissionRequest.reviewTab')}${reviewPendingCount > 0 ? ` (${reviewPendingCount})` : ''}`"
        name="review"
      >
        <ApprovalPanel ref="approvalRef" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import {
  createPermissionRequest,
  getRequestableMenus,
  listMyPermissionRequests,
  parseJsonArray,
  type PermissionRequest,
  type RequestableMenu,
} from '@/api/permission'
import AppPagination from '@/components/AppPagination.vue'
import { MenuType } from '@/api/menu'
import { useUserStore } from '@/stores/user'
import ApprovalPanel from './ApprovalPanel.vue'

defineOptions({ name: 'PermissionRequest' })

const { t } = useI18n()
const userStore = useUserStore()

const tab = ref('mine')

// ===== 可申请菜单树 =====
const treeRef = ref()
const menuTree = ref<RequestableMenu[]>([])
const checkedIds = ref<number[]>([])
const expandedKeys = ref<number[]>([])
const submitLoading = ref(false)
const loadingTree = ref(false)

const allMenuIds = computed(() => {
  const ids: number[] = []
  const collect = (menus: RequestableMenu[]) => {
    menus.forEach((m) => {
      ids.push(m.id)
      if (m.children) collect(m.children)
    })
  }
  collect(menuTree.value)
  return ids
})

const checkedTags = computed(() => {
  const result: RequestableMenu[] = []
  const collect = (menus: RequestableMenu[]) => {
    menus.forEach((m) => {
      if (checkedIds.value.includes(m.id)) result.push(m)
      if (m.children) collect(m.children)
    })
  }
  collect(menuTree.value)
  return result
})

const menuTypeTag = (type?: number) =>
  type === MenuType.DIR ? 'info' : type === MenuType.MENU ? 'success' : type === MenuType.BUTTON ? 'warning' : 'danger'
const menuTypeLabel = (type?: number) =>
  type === MenuType.DIR
    ? t('permissionRequest.typeDir')
    : type === MenuType.MENU
      ? t('permissionRequest.typeMenu')
      : type === MenuType.BUTTON
        ? t('permissionRequest.typeButton')
        : t('permissionRequest.typeTab')

function handleTreeCheck(_cur: unknown, state: { checkedKeys: number[] }) {
  checkedIds.value = state.checkedKeys
}

function removeMenu(menu: RequestableMenu) {
  treeRef.value?.setChecked(menu.id, false, true)
  checkedIds.value = checkedIds.value.filter((id) => id !== menu.id)
}

function expandAll() {
  expandedKeys.value = [...allMenuIds.value]
}

function collapseAll() {
  expandedKeys.value = []
}

async function fetchMenuTree() {
  loadingTree.value = true
  try {
    menuTree.value = (await getRequestableMenus()) || []
    expandedKeys.value = allMenuIds.value.slice(0, 200)
  } finally {
    loadingTree.value = false
  }
}

// ===== 我的申请 =====
const mineLoading = ref(false)
const mineRows = ref<PermissionRequest[]>([])
const mineTotal = ref(0)
const mineCurrent = ref(1)
const mineSize = ref(10)

async function loadMine() {
  mineLoading.value = true
  try {
    const data = await listMyPermissionRequests({ current: mineCurrent.value, size: mineSize.value })
    mineRows.value = data.records || []
    mineTotal.value = data.total || 0
  } finally {
    mineLoading.value = false
  }
}

const parseNames = (raw?: string) => parseJsonArray(raw)
const displayMenus = (req: PermissionRequest) => {
  const names = parseNames(req.menuNames)
  if (names.length) return names.join(t('permissionRequest.menuSeparator'))
  return req.permissionCode || '-'
}

// ===== 提交 =====
async function handleSubmit() {
  if (checkedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(t('permissionRequest.submitConfirm'), t('common.tip'), { type: 'warning' })
  } catch {
    return
  }
  submitLoading.value = true
  try {
    await createPermissionRequest({
      menuIds: checkedIds.value,
      menuNames: checkedTags.value.map((m) => m.menuName),
    })
    ElMessage.success(t('permissionRequest.submitted'))
    checkedIds.value = []
    treeRef.value?.setCheckedKeys([])
    loadMine()
  } catch {
    /* interceptor */
  } finally {
    submitLoading.value = false
  }
}

// ===== 审批 =====
const approvalRef = ref<InstanceType<typeof ApprovalPanel>>()
const reviewPendingCount = computed(() => approvalRef.value?.pendingCount ?? 0)

function onTabClick(pane: { index?: string; name?: string }) {
  const name = typeof pane === 'string' || typeof pane === 'number' ? pane : pane?.name
  if (name === 'review') {
    approvalRef.value?.init()
  }
}

function formatTime(time?: string) {
  return time ? time.replace('T', ' ').slice(0, 19) : '-'
}

const statusTag = (s?: string) => (s === 'PENDING' ? 'warning' : s === 'APPROVED' ? 'success' : 'danger')
const statusLabel = (s?: string) =>
  s === 'PENDING' ? t('permissionRequest.pending') : s === 'APPROVED' ? t('permissionRequest.approved') : t('permissionRequest.rejected')

onMounted(() => {
  fetchMenuTree()
  loadMine()
})
</script>

<style scoped>
.page-tabs :deep(.el-tabs__content) {
  overflow: visible;
}
.section-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}
.tree-node {
  display: flex;
  align-items: center;
  font-size: 14px;
}
.tree-perms {
  color: var(--text-secondary);
  font-size: 11px;
  margin-left: 6px;
}
.empty-hint {
  text-align: center;
  padding: 24px 0;
  font-size: 13px;
  color: var(--text-secondary);
}
.selected-scroll {
  min-height: 60px;
}
.request-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--border-lighter, var(--border-light));
}
.request-item:last-child {
  border-bottom: none;
}
.request-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.request-menus {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 8px;
}
.request-time {
  font-size: 11px;
  color: var(--text-secondary);
}
.request-remark {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}
</style>