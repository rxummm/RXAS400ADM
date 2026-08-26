<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        :placeholder="$t('users.username') + ' / ' + $t('users.email')"
        clearable
        class="w-240"
        @keyup.enter="forceSearch"
      />
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
      <el-button type="success" :icon="Plus" @click="() => openCreate()">
        {{ $t('users.create') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <el-tabs v-model="section">
        <el-tab-pane v-if="userStore.canSeeTab('users', 'usersTab')" :label="$t('users.usersTab')" name="users">
          <RxSkeleton type="table" :rows="8" :loading="loading">
            <el-table :data="users" size="small" border>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="username" :label="$t('users.username')" min-width="130" />
            <el-table-column prop="email" :label="$t('users.email')" min-width="160" />
            <el-table-column :label="$t('users.roles')" min-width="180">
              <template #default="{ row }">
                <el-tag
                  v-for="r in row.roles || []"
                  :key="r.id"
                  size="small"
                  class="mr4"
                >
                  {{ r.roleName || r.roleCode }}
                </el-tag>
                <span v-if="!row.roles || row.roles.length === 0" class="muted">
                  {{ $t('users.noRoles') }}
                </span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('users.loginSource')" width="110">
              <template #default="{ row }">
                <el-tag :type="row.loginSource === 'AS400' ? 'primary' : 'success'" size="small">
                  {{ row.loginSource === 'AS400' ? $t('users.sourceAs400') : $t('users.sourcePlatform') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="$t('common.status')" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ row.status === 'ACTIVE' ? $t('users.statusActive') : $t('users.statusDisabled') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdTime" :label="$t('users.createdTime')" width="180" />
            <el-table-column :label="$t('common.operation')" width="310" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openEdit(row as UserVO)">{{ $t('common.edit') }}</el-button>
                <el-button size="small" type="primary" plain @click="openPermManage(row as UserVO)">
                  {{ $t('users.permManage') }}
                </el-button>
                <el-button
                  size="small"
                  :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
                  plain
                  @click="toggleStatus(row as UserVO)"
                >
                  {{ row.status === 'ACTIVE' ? $t('users.statusDisabled') : $t('users.statusActive') }}
                </el-button>
                <el-button size="small" type="danger" @click="remove(row as UserVO)">
                  {{ $t('common.delete') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          </RxSkeleton>

          <AppPagination
            :total="total"
            v-model:current="current"
            v-model:size="size"
            @change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </el-tab-pane>

        <el-tab-pane v-if="userStore.canSeeTab('users', 'securityTab')" :label="$t('users.securityTab')" name="security">
          <div class="search-bar">
            <el-select
              v-model="attemptServer"
              :placeholder="$t('users.allServers')"
              clearable
              class="w-170"
              @change="loadAttempts"
            >
              <el-option v-for="s in servers" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-button :icon="Refresh" @click="loadAttempts">{{ $t('common.refresh') }}</el-button>
          </div>
          <RxSkeleton type="table" :rows="5" :loading="attemptLoading">
            <el-table :data="attempts" size="small" border>
            <el-table-column prop="username" :label="$t('users.username')" min-width="120" />
            <el-table-column prop="serverId" label="Server" width="80" />
            <el-table-column prop="failedCount" :label="$t('users.failedCount')" width="90" />
            <el-table-column :label="$t('users.lockStatus')" width="150">
              <template #default="{ row }">
                <el-tag v-if="isLocked(row.lockedUntil)" type="danger" size="small">
                  {{ $t('users.lockedTo') }} {{ formatTime(row.lockedUntil) }}
                </el-tag>
                <el-tag v-else type="info" size="small">{{ $t('users.notLocked') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastIp" label="IP" min-width="130" />
            <el-table-column prop="lastFailTime" :label="$t('users.lastFail')" width="170" />
            <el-table-column :label="$t('common.operation')" width="90" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain :loading="removeLoading_unlock === row.username" @click="unlock(row as LoginAttemptRecord)">{{ $t('users.unlock') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          </RxSkeleton>
          <el-divider content-position="left">{{ $t('users.ipStats') }}</el-divider>
          <RxSkeleton type="table" :rows="5" :loading="ipLoading">
            <el-table :data="ipStats" size="small" border>
            <el-table-column prop="ip" label="IP" min-width="150" />
            <el-table-column prop="attempts" :label="$t('users.attempts')" width="100" />
            <el-table-column prop="locked" :label="$t('users.lockedAccounts')" width="110" />
            <el-table-column prop="last_time" :label="$t('users.lastTime')" width="180" />
          </el-table>
          </RxSkeleton>
        </el-tab-pane>
      </el-tabs>
    </div>

    <UserFormDialog v-model="dialogVisible" :edit-data="editData" :roles="roles" @saved="forceSearch" />
    
<UserPermDialog v-model="permManageVisible" :user="permManageUser" @updated="forceSearch" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { useAs400ServerStore } from '@/stores/as400Server'
import {
  deleteUser,
  fetchUsers,
  loginAttemptIps,
  loginAttempts,
  unlockUser,
  updateUser,
  type UserVO,
  type UserStatus,
  type LoginAttemptRecord,
  type IpStat,
} from '@/api/user'
import { fetchRoles } from '@/api/role'
import type { SysRole } from '@/api/role'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import UserFormDialog from './UserFormDialog.vue'
import UserPermDialog from './UserPermDialog.vue'
import { formatDate } from '@/utils/format'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'

defineOptions({ name: 'Users' })

const { t } = useI18n()
const userStore = useUserStore()
const as400Store = useAs400ServerStore()

const {
  tableData: users,
  keyword,
  loading,
  current,
  size,
  total,
  forceSearch,
  handlePageChange,
  handleSizeChange,
} = useSmartQueryTable<UserVO>({
  fetchApi: async (params) => {
    const data = await fetchUsers({ current: params.current, size: params.size, keyword: params.keyword })
    return { records: data.records, total: data.total }
  },
  frontendPage: false,
  enableCache: false,
})

const section = ref('users')
const servers = ref<{ id: number; name: string }[]>([])
const attempts = ref<LoginAttemptRecord[]>([])
const attemptLoading = ref(false)
const attemptServer = ref<number | undefined>(undefined)
const ipStats = ref<IpStat[]>([])
const ipLoading = ref(false)
const dialogVisible = ref(false)
const editData = ref<{ id: number; username: string; email: string; roles: SysRole[]; status: UserStatus } | null>(null)
const roles = ref<SysRole[]>([])

const permManageVisible = ref(false)
const permManageUser = ref<{ id: number; username: string }>({ id: 0, username: '' })

const loadRoles = async () => {
  try {
    roles.value = (await fetchRoles()) as SysRole[]
  } catch {
    roles.value = []
  }
}

const isLocked = (lockedUntil?: string) => !!lockedUntil && new Date(lockedUntil) > new Date()

const formatTime = (v?: string) => formatDate(v) || '-'

const loadAttempts = async () => {
  attemptLoading.value = true
  try {
    attempts.value = await loginAttempts(attemptServer.value)
  } finally {
    attemptLoading.value = false
  }
}

const loadIpStats = async () => {
  ipLoading.value = true
  try {
    ipStats.value = await loginAttemptIps()
  } finally {
    ipLoading.value = false
  }
}

const removeLoading_unlock = ref<string | null>(null)
const unlock = async (row: LoginAttemptRecord) => {
  try {
    await ElMessageBox.confirm(
      t('users.unlockConfirm', { username: row.username }),
      t('common.confirm'),
      { type: 'warning' },
    )
  } catch {
    return
  }
  removeLoading_unlock.value = row.username
  try {
    await unlockUser(row.username, row.serverId)
    ElMessage.success(t('users.unlocked'))
    await loadAttempts()
    await loadIpStats()
  } finally {
    removeLoading_unlock.value = null
  }
}

const openCreate = () => {
  editData.value = null
  dialogVisible.value = true
}

const openEdit = (row: UserVO) => {
  editData.value = {
    id: row.id,
    username: row.username,
    email: row.email || '',
    roles: row.roles || [],
    status: row.status,
  }
  dialogVisible.value = true
}

const toggleStatus = async (row: UserVO) => {
  const next = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await updateUser(row.id, { status: next })
  ElMessage.success(t('users.updateSuccess'))
  await forceSearch()
}

const removeLoading_remove = ref<number | null>(null)
const remove = async (row: UserVO) => {
  try {
    await ElMessageBox.confirm(
      t('users.deleteConfirm', { username: row.username }),
      t('common.confirm'),
      { type: 'warning' },
    )
  } catch {
    return
  }
  removeLoading_remove.value = row.id
  try {
    await deleteUser(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    await forceSearch()
  } finally {
    removeLoading_remove.value = null
  }
}

function openPermManage(row: UserVO) {
  permManageUser.value = { id: row.id, username: row.username }
  permManageVisible.value = true
}

onMounted(async () => {
  loadRoles()
  servers.value = await as400Store.fetchServers()
  loadAttempts()
  loadIpStats()
})
</script>

<style scoped>
.muted {
  color: var(--text-placeholder);
  font-size: 12px;
}
</style>