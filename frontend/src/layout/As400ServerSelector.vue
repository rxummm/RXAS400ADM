<template>
  <el-dropdown trigger="click" @command="onSelect">
    <div class="server-trigger">
      <el-icon :size="14"><Link /></el-icon>
      <span class="server-name">{{ currentName }}</span>
      <el-icon :size="12"><ArrowDown /></el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-for="server in serverList" :key="server.id" :command="server.id">
          <span class="dot" :class="dotClass(server)"></span>
          <span>{{ server.name }}</span>
          <el-tag v-if="server.defaultServer" size="small" type="success" class="ml2">
            {{ $t('as400Server.defaultTag') }}
          </el-tag>
          <span class="host">{{ server.host }}:{{ server.port }}</span>
        </el-dropdown-item>
        <el-dropdown-item divided disabled>
          {{ $t('as400Server.hint') }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { ArrowDown, Link } from '@element-plus/icons-vue'
import { useAs400ServerStore, type As400Server } from '@/stores/as400Server'

const store = useAs400ServerStore()

const serverList = computed(() => store.serverList)
const currentName = computed(() => store.currentServer?.name || '')

const dotClass = (server: As400Server) =>
  server.status === 'ONLINE' ? 'online' : server.enabled === false ? 'disabled' : 'unknown'

const onSelect = (id: number) => {
  store.setCurrentServer(id)
}

onMounted(() => {
  if (serverList.value.length === 0) {
    store.fetchServers()
  }
})
</script>

<style scoped>
.server-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 0 8px;
  font-size: 13px;
}
.server-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  margin-right: 4px;
}
.dot.online {
  background: var(--color-success);
}
.dot.disabled {
  background: var(--color-info);
}
.dot.unknown {
  background: var(--color-warning);
}
.host {
  color: var(--text-secondary);
  font-size: 12px;
  margin-left: 8px;
}
.ml2 {
  margin-left: 6px;
}
</style>