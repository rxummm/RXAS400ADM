<template>
  <div class="page-container page-container--fit">
    <div class="table-wrapper">
      <el-tabs v-if="tabs.length" v-model="active" type="border-card">
        <el-tab-pane v-if="userStore.hasPermission('SYS_CONFIG_MANAGE')" name="config">
          <template #label>
            <el-icon class="mr4"><Setting /></el-icon>{{ $t('menu.config') }}
          </template>
          <SysConfig />
        </el-tab-pane>
        <el-tab-pane v-if="userStore.hasPermission('DICT_MANAGE')" name="dict">
          <template #label>
            <el-icon class="mr4"><Collection /></el-icon>{{ $t('menu.dict') }}
          </template>
          <DictManage />
        </el-tab-pane>
        <el-tab-pane v-if="userStore.hasPermission('PERMISSION_MANAGE')" name="permissions">
          <template #label>
            <el-icon class="mr4"><Key /></el-icon>{{ $t('menu.permissions') }}
          </template>
          <Permissions />
        </el-tab-pane>
      </el-tabs>
      <el-empty v-else :description="$t('params.noPermission')" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Collection, Key, Setting } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import SysConfig from '@/views/system/config/index.vue'
import DictManage from '@/views/system/dict/index.vue'
import Permissions from '@/views/system/permissions/index.vue'

defineOptions({ name: 'SysParams' })

const userStore = useUserStore()

const tabs = computed(() => {
  const list: string[] = []
  if (userStore.hasPermission('SYS_CONFIG_MANAGE')) list.push('config')
  if (userStore.hasPermission('DICT_MANAGE')) list.push('dict')
  if (userStore.hasPermission('PERMISSION_MANAGE')) list.push('permissions')
  return list
})

const active = ref(tabs.value[0] || 'config')
</script>