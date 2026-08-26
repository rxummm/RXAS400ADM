<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-checkbox v-model="unreadOnly" @change="load">
        {{ $t('notification.title') }}：{{ $t('notification.unread', { count: unreadCount }) }}
      </el-checkbox>
      <div class="flex-1" />
      <el-button v-if="canDelete" type="danger" plain :disabled="!selectedIds.length" :loading="removeLoading_onBatchDelete === 'batch'" @click="onBatchDelete">
        <el-icon><Delete /></el-icon> {{ $t('common.batchDelete') }} ({{ selectedIds.length }})
      </el-button>
      <el-button type="primary" plain @click="markAllRead">
        <el-icon><Check /></el-icon> {{ $t('notification.markAllRead') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="rows" size="small" border stripe class="w-full" @selection-change="onSelectionChange">
        <el-table-column v-if="canDelete" type="selection" width="44" align="center" />
        <el-table-column prop="type" label="Type" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.type)">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" :label="$t('notice.title')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'notif-unread': row.readFlag === 0 }">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" :label="$t('notice.content')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="readFlag" :label="$t('common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.readFlag === 1 ? 'info' : 'warning'">
              {{ row.readFlag === 1 ? $t('common.read') : $t('common.unread') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" :label="$t('notification.time')" width="170">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" :width="canDelete ? 150 : 100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.readFlag === 0" link type="primary" size="small" @click="onRead(row as Notification)">
              {{ $t('notification.markRead') }}
            </el-button>
            <el-button v-if="canDelete" link type="danger" size="small" :loading="removeLoading_onDelete === row.id" @click="onDelete(row as Notification)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="load" @size-change="load" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Check, Delete } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listMyNotifications,
  getUnreadCount,
  markNotificationRead,
  markAllNotificationsRead,
  deleteNotification,
  deleteNotifications,
  type Notification,
} from '@/api/notification'
import { useUserStore } from '@/stores/user'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'

defineOptions({ name: 'Notifications' })

const { t } = useI18n()
const userStore = useUserStore()

const loading = ref(false)
const rows = ref<Notification[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const unreadOnly = ref(false)
const unreadCount = ref(0)
const selectedIds = ref<number[]>([])

const canDelete = computed(() => userStore.hasPermission('NOTIFICATION_MANAGE'))

function formatTime(time?: string) {
  return time ? time.replace('T', ' ').slice(0, 19) : '-'
}

const typeTag = (type: string): 'danger' | 'success' | 'warning' | 'info' =>
  ({ ALERT: 'danger', NOTICE: 'success', PERMISSION: 'warning', SYSTEM: 'info' } as Record<string, 'danger' | 'success' | 'warning' | 'info'>)[type] || 'info'
const typeLabel = (type: string) =>
  type === 'ALERT'
    ? t('notification.typeAlert')
    : type === 'NOTICE'
      ? t('notification.typeNotice')
      : type === 'PERMISSION'
        ? t('notification.typePermission')
        : t('notification.typeSystem')

async function load() {
  loading.value = true
  try {
    const data = await listMyNotifications({
      current: current.value,
      size: size.value,
      unreadOnly: unreadOnly.value,
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function refreshUnread() {
  try {
    const data = await getUnreadCount()
    unreadCount.value = data.count || 0
  } catch {
    /* interceptor 已提示错误 */
  }
}

async function onRead(row: Notification) {
  if (row.id) {
    await markNotificationRead(row.id)
    row.readFlag = 1
    refreshUnread()
  }
}

const onSelectionChange = (selection: Notification[]) => {
  selectedIds.value = selection.map((s) => s.id as number)
}

const removeLoading_onDelete = ref<number>()
async function onDelete(row: Notification) {
  try {
    await ElMessageBox.confirm(t('notification.deleteConfirm'), t('common.tip'), { type: 'warning' })
  } catch {
    return
  }
  removeLoading_onDelete.value = row.id
  try {
    if (row.id) await deleteNotification(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    refreshUnread()
    load()
  } finally {
    removeLoading_onDelete.value = undefined
  }
}

const removeLoading_onBatchDelete = ref<string | null>(null)
async function onBatchDelete() {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(t('notification.batchDeleteConfirm', { count: selectedIds.value.length }), t('common.tip'), { type: 'warning' })
  } catch {
    return
  }
  removeLoading_onBatchDelete.value = 'batch'
  try {
    await deleteNotifications(selectedIds.value)
    ElMessage.success(t('common.deleteSuccess'))
    selectedIds.value = []
    refreshUnread()
    load()
  } finally {
    removeLoading_onBatchDelete.value = null
  }
}

async function markAllRead() {
  await markAllNotificationsRead()
  ElMessage.success(t('notification.readAllDone'))
  refreshUnread()
  load()
}

onMounted(() => {
  load()
  refreshUnread()
})
</script>

<style scoped>
.notif-unread {
  font-weight: 600;
}
</style>