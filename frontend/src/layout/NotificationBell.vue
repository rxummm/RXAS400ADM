<template>
  <el-popover placement="bottom" :width="340" trigger="click" @show="loadRecent">
    <template #reference>
      <div class="notif-bell" :title="$t('notification.title')">
        <el-icon :size="16"><Bell /></el-icon>
        <el-badge v-if="unread > 0" :value="unread > 99 ? '99+' : unread" :max="99" class="notif-badge" />
      </div>
    </template>
    <div class="notif-panel">
      <div class="notif-header">
        <span class="notif-title">{{ $t('notification.title') }}</span>
        <el-button link type="primary" size="small" @click="markAllRead">
          {{ $t('notification.markAllRead') }}
        </el-button>
      </div>
      <div v-loading="recentLoading" class="notif-list">
        <template v-if="recent.length > 0">
          <div
            v-for="item in recent"
            :key="item.id"
            class="notif-item"
            :class="{ unread: item.readFlag === 0 }"
            @click="onItemClick(item)"
          >
            <el-tag size="small" :type="typeTag(item.type || '')" class="notif-type">
              {{ typeLabel(item.type || '') }}
            </el-tag>
            <div class="notif-body">
              <div class="notif-text">{{ item.title }}</div>
              <div class="notif-time">{{ formatTime(item.createdTime) }}</div>
            </div>
          </div>
        </template>
        <el-empty v-else :description="$t('notification.empty')" :image-size="60" />
      </div>
      <div class="notif-footer">
        <el-button link type="primary" size="small" @click="goAll">
          {{ $t('notification.viewAll') }}
        </el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElNotification } from 'element-plus'
import { useStompClient } from '@/composables/useStompClient'
import {
  getUnreadCount,
  listMyNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  type Notification,
} from '@/api/notification'

const router = useRouter()
const { t } = useI18n()

const unread = ref(0)
const recent = ref<Notification[]>([])
const recentLoading = ref(false)
const { connect: connectStomp } = useStompClient()

function formatTime(time?: string) {
  return time ? time.replace('T', ' ').slice(0, 19) : '-'
}

const typeTag = (type: string) =>
  ({ ALERT: 'danger', NOTICE: 'success', PERMISSION: 'warning', SYSTEM: 'info' } as Record<string, string>)[type || ''] || 'info'
const typeLabel = (type: string) =>
  type === 'ALERT'
    ? t('notification.typeAlert')
    : type === 'NOTICE'
      ? t('notification.typeNotice')
      : type === 'PERMISSION'
        ? t('notification.typePermission')
        : t('notification.typeSystem')

async function refreshUnread() {
  try {
    const data = await getUnreadCount()
    unread.value = data.count || 0
  } catch {
    /* ignore */
  }
}

async function loadRecent() {
  recentLoading.value = true
  try {
    const data = await listMyNotifications({ current: 1, size: 8 })
    recent.value = data.records || []
  } finally {
    recentLoading.value = false
  }
}

async function onItemClick(item: Notification) {
  if (item.id && item.readFlag === 0) {
    await markNotificationRead(item.id)
    item.readFlag = 1
    unread.value = Math.max(0, unread.value - 1)
  }
}

async function markAllRead() {
  await markAllNotificationsRead()
  recent.value.forEach((n) => (n.readFlag = 1))
  unread.value = 0
}

function goAll() {
  router.push('/system/notifications')
}

// P2-26：按 STOMP message-id 去重（重连补推/重复投递不重复累计未读数）
const seenMessageIds = new Set<string>()
const MAX_SEEN_MESSAGE_IDS = 300

function connectSocket() {
  connectStomp((client) => {
    client.subscribe('/user/queue/notifications', (message) => {
      const msgId = message.headers?.['message-id'] || ''
      if (msgId) {
        if (seenMessageIds.has(msgId)) return
        seenMessageIds.add(msgId)
        if (seenMessageIds.size > MAX_SEEN_MESSAGE_IDS) seenMessageIds.clear()
      }
      try {
        const data = JSON.parse(message.body) as Notification
        unread.value += 1
        ElNotification({
          title: data.title || t('notification.title'),
          message: data.content || '',
          type: data.type === 'ALERT' ? 'warning' : 'info',
          duration: 4500,
        })
      } catch {
        /* ignore */
      }
    })
  })
}

onMounted(() => {
  refreshUnread()
  connectSocket()
})
</script>

<style scoped>
.notif-bell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--text-regular);
  transition: all 0.15s;
}
.notif-bell:hover {
  background: var(--bg-hover);
  color: var(--color-primary);
}
.notif-badge {
  position: absolute;
  transform: translate(-4px, -10px);
}
.notif-panel {
  display: flex;
  flex-direction: column;
  max-height: 380px;
}
.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}
.notif-title {
  font-weight: 600;
}
.notif-list {
  flex: 1;
  overflow-y: auto;
  min-height: 80px;
}
.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 4px;
  border-bottom: 1px solid var(--border-light);
  cursor: pointer;
}
.notif-item:hover {
  background: var(--bg-hover);
}
.notif-item.unread .notif-text {
  font-weight: 600;
}
.notif-type {
  flex-shrink: 0;
  margin-top: 2px;
}
.notif-body {
  min-width: 0;
}
.notif-text {
  font-size: 13px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.notif-time {
  font-size: 12px;
  color: var(--text-placeholder);
  margin-top: 2px;
}
.notif-footer {
  display: flex;
  justify-content: center;
  padding-top: 6px;
  border-top: 1px solid var(--border-light);
}
</style>