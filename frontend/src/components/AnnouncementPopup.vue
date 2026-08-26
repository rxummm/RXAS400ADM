<template>
  <el-dialog
    v-model="visible"
    :title="current?.title || $t('announcement.title')"
    width="var(--rx-dialog-sm)"
    :close-on-click-modal="true"
    @close="markRead"
  >
    <div class="announce-content">{{ current?.content }}</div>
    <template #footer>
      <el-button type="primary" @click="markRead">{{ $t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listPublishedNotices, type Notice } from '@/api/notice'
import { useStorage, STORAGE_KEYS } from '@/composables/useStorage'

defineOptions({ name: 'AnnouncementPopup' })

const visible = ref(false)
const current = ref<Notice | null>(null)
const readStore = useStorage(STORAGE_KEYS.NOTICE_READ)

function markRead() {
  if (current.value?.id) {
    readStore.setJson([...(readStore.getJson<number[]>([]) || []), current.value.id])
  }
  visible.value = false
}

async function check() {
  try {
    const list = (await listPublishedNotices()) || []
    if (list.length === 0) return
    // 取最新发布的一条（未读）
    const latest = list[0]
    const read = readStore.getJson<number[]>([]) || []
    if (latest.id && !read.includes(latest.id)) {
      current.value = latest
      visible.value = true
    }
  } catch {
    /* 静默：无公告/接口不可达时不打扰登录 */
  }
}

onMounted(check)

defineExpose({ check })
</script>

<style scoped>
.announce-content {
  white-space: pre-wrap;
  line-height: 1.7;
  color: var(--text-primary);
  max-height: 360px;
  overflow-y: auto;
}
</style>
