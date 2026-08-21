<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="$t('sysvals.keyword')"
      :from-cache="isFromCache"
      :flash-tick="dataSourceTick"
      :keyword-width="240"
      @force-search="handleRefresh"
      @reset="resetSearch"
    >
      <template #right>
        <el-radio-group v-model="filterMode" size="small">
          <el-radio-button value="all">{{ $t('sysvals.all') }}</el-radio-button>
          <el-radio-button value="common">{{ $t('sysvals.common') }}</el-radio-button>
          <el-radio-button value="fav">{{ $t('sysvals.myFavs') }}</el-radio-button>
        </el-radio-group>
        <span class="hint">{{ $t('sysvals.hint') }}</span>
      </template>
    </QueryBar>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="visibleRows" size="small" border>
        <el-table-column width="56" align="center">
          <template #default="{ row }: { row: SystemValue }">
            <el-tooltip :content="isFav(row.SYSTEM_VALUE_NAME) ? $t('sysvals.unfav') : $t('sysvals.fav')" placement="top">
              <el-button
                text
                size="small"
                :icon="isFav(row.SYSTEM_VALUE_NAME) ? StarFilled : Star"
                :style="{ color: isFav(row.SYSTEM_VALUE_NAME) ? 'var(--el-color-warning)' : '' }"
                @click="toggleFav(row.SYSTEM_VALUE_NAME)"
              />
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="SYSTEM_VALUE_NAME" :label="$t('sysvals.name')" width="180">
          <template #default="{ row }: { row: SystemValue }">
            <b>{{ row.SYSTEM_VALUE_NAME }}</b>
            <el-tag v-if="isCommon(row.SYSTEM_VALUE_NAME)" size="small" type="info" class="ml8">
              {{ $t('sysvals.common') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="CURRENT_VALUE" :label="$t('sysvals.currentValue')" min-width="140">
          <template #default="{ row }: { row: SystemValue }">
            <el-tag size="small">{{ row.CURRENT_VALUE }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="VALUE_DESCRIPTION" :label="$t('sysvals.description')" min-width="260" show-overflow-tooltip />
        <el-table-column prop="SYSTEM_VALUE_TYPE" :label="$t('sysvals.type')" width="90" align="center" />
        <el-table-column :label="$t('common.operation')" width="100" align="center" fixed="right">
          <template #default="{ row }: { row: SystemValue }">
            <el-button v-has-perm="'SYSVAL_EDIT'" size="small" type="primary" plain :icon="Edit" @click="openEdit(row)">
              {{ $t('sysvals.modify') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <el-empty v-if="!loading && !visibleRows.length" :description="$t('common.noData')" />
    </div>

    <el-dialog v-model="dialogVisible" :title="`${$t('sysvals.modify')}：${form.name}`" width="420px">
      <el-form :model="form" label-width="120px">
        <el-form-item :label="$t('sysvals.currentValue')">
          <el-input v-model="form.value" :placeholder="$t('sysvals.valuePlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Star, StarFilled } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { fetchSystemValues, updateSystemValue, type SystemValue } from '@/api/systemValues'
import QueryBar from '@/components/QueryBar.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t } = useI18n()
const userStore = useUserStore()

/** 常用系统值（业界高频关注项，用于快速筛选/收藏引导） */
const COMMON_SYSVALS = [
  'QCCSID', 'QDATE', 'QTIME', 'QHOUR', 'QSYSNAME', 'QMODEL', 'QPRCFEAT',
  'QTOTMEM', 'QMLTTHDACN', 'QLANGID', 'QCFGMSGQ', 'QSPLFACN', 'QCENTURY',
  'QABNORMSW', 'QMAXSGNACN', 'QPFRADJ', 'QACTJOB', 'QADLACTJ', 'QJOBMSGQFL',
  'QHSTLOGQ',
]

const filterMode = ref<'all' | 'common' | 'fav'>('all')

const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({ name: '', value: '' })

/** 收藏：localStorage 按用户隔离（key: rxas400adm:sysval-favs:<username>） */
const FAV_KEY = `rxas400adm:sysval-favs:${userStore.username || 'anonymous'}`
const favs = ref<Set<string>>(new Set())

const readFavs = () => {
  try {
    const raw = JSON.parse(localStorage.getItem(FAV_KEY) || '[]')
    favs.value = new Set(Array.isArray(raw) ? raw : [])
  } catch {
    favs.value = new Set()
  }
}

const persistFavs = () => {
  localStorage.setItem(FAV_KEY, JSON.stringify([...favs.value]))
}

const isFav = (name: string) => favs.value.has(name)
const isCommon = (name: string) => COMMON_SYSVALS.includes(String(name || '').toUpperCase())

const toggleFav = (name: string) => {
  if (favs.value.has(name)) {
    favs.value.delete(name)
  } else {
    favs.value.add(name)
  }
  persistFavs()
}

// 3 分钟查询缓存 + 关键词前端实时模糊匹配；常用/收藏在关键词之上再叠加过滤
const {
  filteredData,
  loading,
  keyword,
  isFromCache,
  dataSourceTick,
  resetSearch,
  handleRefresh,
  fetchData,
} = useSmartQueryTable<SystemValue>({
  fetchApi: () => fetchSystemValues(),
  enableCache: true,
  searchFields: ['SYSTEM_VALUE_NAME', 'CURRENT_VALUE', 'VALUE_DESCRIPTION', 'SYSTEM_VALUE_TYPE'],
})

const visibleRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return filteredData.value.filter((row) => {
    if (filterMode.value === 'common' && !isCommon(row.SYSTEM_VALUE_NAME)) return false
    if (filterMode.value === 'fav' && !isFav(row.SYSTEM_VALUE_NAME)) return false
    // keyword 空时 composable 不调用 searchFields，这里兜底
    if (kw) {
      return ['SYSTEM_VALUE_NAME', 'CURRENT_VALUE', 'VALUE_DESCRIPTION', 'SYSTEM_VALUE_TYPE'].some((f) => {
        const v = row[f]
        return v != null && String(v).toLowerCase().includes(kw)
      })
    }
    return true
  })
})

const openEdit = (row: SystemValue) => {
  form.name = row.SYSTEM_VALUE_NAME
  form.value = row.CURRENT_VALUE
  dialogVisible.value = true
}

const handleSave = async () => {
  saving.value = true
  try {
    await ElMessageBox.confirm(
      `${t('sysvals.changeConfirm')} ${form.name} = ${form.value}？`,
      t('common.warning'),
      { type: 'warning' },
    )
    await updateSystemValue(form.name, form.value)
    ElMessage.success(t('sysvals.changed'))
    dialogVisible.value = false
    void fetchData({}, true)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  readFavs()
})
</script>

<style scoped>
/* 通用样式收敛至 src/styles/common.css */
</style>