<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="`${$t('common.keyword')}: ${$t('scripts.name')} / ${$t('scripts.command')}`"
      :from-cache="isFromCache"
      :flash-tick="dataSourceTick"
      :keyword-width="220"
      @force-search="onFilterChange"
      @reset="resetSearch"
    >
      <el-select v-model="tagFilter" :placeholder="$t('scripts.tagFilter')" clearable class="w-140" @change="onFilterChange">
        <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
      </el-select>
      <el-checkbox v-model="favOnly" @change="onFilterChange">{{ $t('scripts.favOnly') }}</el-checkbox>
      <template #right>
        <el-button type="primary" :icon="Plus" @click="openCreate">{{ $t('scripts.create') }}</el-button>
      </template>
    </QueryBar>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedData" size="small" border>
        <el-table-column width="60">
          <template #default="{ row }: { row: CommandScript }">
            <el-button
              link
              :type="row.favorite ? 'warning' : 'info'"
              :icon="row.favorite ? StarFilled : Star"
              @click="toggleFav(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('scripts.name')" min-width="130" />
        <el-table-column prop="command" :label="$t('scripts.command')" min-width="220" show-overflow-tooltip />
        <el-table-column :label="$t('scripts.tags')" width="150">
          <template #default="{ row }: { row: CommandScript }">
            <el-tag v-for="tag in splitTags(row.tags)" :key="tag" size="small" class="mr4">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="runCount" :label="$t('scripts.runCount')" width="80" />
        <el-table-column prop="lastRunTime" :label="$t('scripts.lastRun')" width="170">
          <template #default="{ row }: { row: CommandScript }">{{ row.lastRunTime || '-' }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="230" fixed="right">
          <template #default="{ row }: { row: CommandScript }">
            <el-select v-model="runServer[row.id]" :placeholder="$t('scripts.selectServer')" size="small" class="w-120">
              <el-option v-for="s in servers" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-button v-has-perm="'SCRIPT_MANAGE'" size="small" type="primary" plain :loading="runningId === row.id" @click="run(row)">
              {{ $t('scripts.run') }}
            </el-button>
            <el-button v-has-perm="'SCRIPT_MANAGE'" size="small" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-button v-has-perm="'SCRIPT_MANAGE'" size="small" type="danger" plain @click="remove(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? $t('scripts.edit') : $t('scripts.create')" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item :label="$t('scripts.name')" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="$t('scripts.command')" required>
          <el-input v-model="form.command" type="textarea" :rows="4" placeholder="WRKACTJOB" />
        </el-form-item>
        <el-form-item :label="$t('scripts.tags')">
          <el-input v-model="form.tags" :placeholder="$t('scripts.tagsHint')" />
        </el-form-item>
        <el-form-item :label="$t('scripts.favorite')">
          <el-switch v-model="form.favorite" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Star, StarFilled } from '@element-plus/icons-vue'
import QueryBar from '@/components/QueryBar.vue'
import { useI18n } from 'vue-i18n'
import { useAs400ServerStore, type As400Server } from '@/stores/as400Server'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import {
  createScript,
  deleteScript,
  executeScript,
  listScripts,
  listScriptTags,
  toggleScriptFavorite,
  updateScript,
  type CommandScript,
  type ScriptRunResult,
} from '@/api/script'

const { t } = useI18n()
const as400Store = useAs400ServerStore()
const allTags = ref<string[]>([])
const servers = ref<As400Server[]>([])
const saving = ref(false)
const runningId = ref(0)
const tagFilter = ref<string | null>(null)
const favOnly = ref(false)
const runServer = reactive<Record<number, number>>({})

const {
  pagedData,
  loading,
  keyword,
  current,
  size,
  total,
  isFromCache,
  dataSourceTick,
  resetSearch,
  handlePageChange,
  handleSizeChange,
  fetchData,
} = useSmartQueryTable<CommandScript>({
  fetchApi: (params) =>
    listScripts({
      favorite: (params.favorite as boolean) || undefined,
      tag: (params.tag as string) || undefined,
    }),
  frontendPage: true,
  enableCache: true,
  searchFields: ['name', 'command', 'tags'],
  buildParams: () => ({
    favorite: favOnly.value || undefined,
    tag: tagFilter.value || undefined,
  }),
})

const onFilterChange = () => {
  current.value = 1
  void fetchData({}, true)
}

const dialogVisible = ref(false)
const form = ref<Partial<CommandScript> & { id?: number }>({ name: '', command: '', tags: '', favorite: false })

const splitTags = (tags?: string) => (tags || '').split(',').map((s) => s.trim()).filter(Boolean)

const loadTags = async () => {
  try {
    allTags.value = (await listScriptTags()) as string[]
  } catch {
    allTags.value = []
  }
}

const openCreate = () => {
  form.value = { name: '', command: '', tags: '', favorite: false }
  dialogVisible.value = true
}

const openEdit = (row: CommandScript) => {
  form.value = { id: row.id, name: row.name, command: row.command, tags: row.tags || '', favorite: row.favorite }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.name || !form.value.command) {
    ElMessage.warning(t('scripts.required'))
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await updateScript(form.value.id, form.value)
    } else {
      await createScript(form.value)
    }
    ElMessage.success(t('common.save'))
    dialogVisible.value = false
    await fetchData({}, true)
  } finally {
    saving.value = false
  }
}

const toggleFav = async (row: CommandScript) => {
  await toggleScriptFavorite(row.id, !row.favorite)
  await fetchData({}, true)
}

const run = async (row: CommandScript) => {
  const serverId = runServer[row.id]
  if (!serverId) {
    ElMessage.warning(t('scripts.selectServerFirst'))
    return
  }
  runningId.value = row.id
  try {
    const result: ScriptRunResult = await executeScript(row.id, serverId)
    ElMessage[result.success ? 'success' : 'error'](result.message)
    await fetchData({}, true)
  } finally {
    runningId.value = 0
  }
}

const remove = async (row: CommandScript) => {
  await ElMessageBox.confirm(t('scripts.deleteConfirm'), t('common.confirm'), { type: 'warning' })
  await deleteScript(row.id)
  ElMessage.success(t('common.delete'))
  await fetchData({}, true)
}

onMounted(async () => {
  servers.value = await as400Store.fetchServers()
  loadTags()
})
</script>

<style scoped>
/* card-header/ml8/mr4 已收敛至 common.css */
</style>