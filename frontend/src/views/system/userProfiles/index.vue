<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" :loading="loading" @click="load">
        {{ $t('common.refresh') }}
      </el-button>
      <el-button v-has-perm="'USER_MANAGE'" type="warning" @click="switchVisible = true">
        {{ $t('userProfile.switchUser') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <el-table :data="profiles" v-loading="loading" size="small" border>
        <el-table-column prop="USER_NAME" :label="$t('userProfile.userName')" min-width="140" />
        <el-table-column prop="STATUS" :label="$t('userProfile.status')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.STATUS === '*ENABLED' ? 'success' : 'danger'" size="small">
              {{ row.STATUS }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="GROUP_PROFILE" :label="$t('userProfile.groupProfile')" width="120" />
        <el-table-column prop="TEXT_DESCRIPTION" :label="$t('userProfile.description')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="LAST_USED_DATE" :label="$t('userProfile.lastUsed')" width="120" />
      </el-table>
      <el-empty v-if="!loading && profiles.length === 0" :description="$t('common.noData')" />
    </div>

    <!-- 切换用户态弹窗 -->
    <el-dialog
      v-model="switchVisible"
      :title="$t('userProfile.switchTitle')"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form label-width="80px">
        <el-form-item :label="$t('userProfile.targetUser')">
          <el-input v-model="switchTarget" :placeholder="$t('userProfile.targetUserPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="switchVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="switching" @click="handleSwitch">
          {{ $t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'UserProfiles' })

import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { fetchUserProfiles, switchUser, type UserProfileList } from '@/api/userProfiles'

const { t } = useI18n()
const loading = ref(false)
const profiles = ref<UserProfileList[]>([])
const switchVisible = ref(false)
const switchTarget = ref('')
const switching = ref(false)

function load() {
  loading.value = true
  fetchUserProfiles()
    .then(data => { profiles.value = data })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

async function handleSwitch() {
  if (!switchTarget.value.trim()) return
  switching.value = true
  try {
    await switchUser(switchTarget.value.trim())
    ElMessage.success(t('userProfile.switchSuccess'))
    switchVisible.value = false
    load()
  } catch {
    /* interceptor handles error */
  } finally {
    switching.value = false
  }
}

load()
</script>
