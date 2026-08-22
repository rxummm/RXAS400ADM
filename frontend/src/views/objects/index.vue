<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="$t('common.keyword')"
      :from-cache="isFromCache"
      :flash-tick="dataSourceTick"
      :keyword-width="160"
      @force-search="handleRefresh"
      @reset="resetSearch"
    >
      <el-input v-model="library" :placeholder="$t('objects.library')" clearable class="w-150" @keyup.enter="handleRefresh" />
      <el-select v-model="type" :placeholder="$t('objects.type')" clearable class="w-130" @change="handleRefresh">
        <el-option v-for="tp in types" :key="tp" :label="tp" :value="tp" />
      </el-select>
    </QueryBar>

    <el-card shadow="never">
      <template #header>
        <span>{{ $t('objects.result') }}</span>
        <span v-if="total" class="count">{{ total }} {{ $t('objects.rows') }}</span>
      </template>
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedData" size="small" border>
        <el-table-column prop="OBJECT_NAME" :label="$t('objects.name')" min-width="140" />
        <el-table-column prop="OBJECT_TYPE" :label="$t('objects.type')" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.OBJECT_TYPE }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="OBJECT_LIBRARY" :label="$t('objects.library')" min-width="120" />
        <el-table-column prop="OBJECT_SIZE" :label="$t('objects.size')" width="120">
          <template #default="{ row }">{{ formatSize(row.OBJECT_SIZE) }}</template>
        </el-table-column>
        <el-table-column prop="OBJECT_CREATION_TIMESTAMP" :label="$t('objects.created')" width="180" />
        <el-table-column :label="$t('common.operation')" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openDetail(row)">{{ $t('objects.detail') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <el-empty v-if="!loading && total === 0" :description="$t('objects.empty')" />
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="handlePageChange" @size-change="handleSizeChange" />
    </el-card>

    <el-drawer v-model="drawerVisible" :title="`${detailRow?.OBJECT_NAME} (${detailRow?.OBJECT_LIBRARY})`" size="560px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item :label="$t('objects.name')">{{ detail.OBJECT_NAME }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.type')">{{ detail.OBJECT_TYPE }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.library')">{{ detail.OBJECT_LIBRARY }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.size')">{{ formatSize(detail.OBJECT_SIZE) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.created')">{{ detail.OBJECT_CREATION_TIMESTAMP }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.changed')">{{ detail.OBJECT_CHANGE_TIMESTAMP || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.text')">{{ detail.OBJECT_TEXT_DESCRIPTION || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.owner')">{{ detail.OBJECT_OWNER || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.asp')">{{ detail.ASP_NAME || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="refTab" class="mt16">
          <el-tab-pane v-if="userStore.canSeeTab('objects', 'refIn')" :label="$t('objects.refIn')" name="IN">
            <RxSkeleton type="table" :rows="5" :loading="refLoading">
              <el-table :data="refIn" size="small" border>
              <el-table-column prop="OBJECT_NAME" :label="$t('objects.name')" min-width="120" />
              <el-table-column prop="OBJECT_TYPE" :label="$t('objects.type')" width="90" />
              <el-table-column prop="OBJECT_LIBRARY" :label="$t('objects.library')" min-width="100" />
            </el-table>
            </RxSkeleton>
            <el-empty v-if="!refLoading && refIn.length === 0" :description="$t('objects.noRef')" />
          </el-tab-pane>
          <el-tab-pane v-if="userStore.canSeeTab('objects', 'refOut')" :label="$t('objects.refOut')" name="OUT">
            <RxSkeleton type="table" :rows="5" :loading="refLoading">
              <el-table :data="refOut" size="small" border>
              <el-table-column prop="REF_OBJ_NAME" :label="$t('objects.name')" min-width="120" />
              <el-table-column prop="REF_OBJ_TYPE" :label="$t('objects.type')" width="90" />
              <el-table-column prop="REF_OBJ_LIBRARY" :label="$t('objects.library')" min-width="100" />
            </el-table>
            </RxSkeleton>
            <el-empty v-if="!refLoading && refOut.length === 0" :description="$t('objects.noRef')" />
          </el-tab-pane>
          <el-tab-pane v-if="userStore.canSeeTab('objects', 'authorities')" :label="$t('objects.authorities')" name="AUTH">
            <RxSkeleton type="table" :rows="5" :loading="authLoading">
              <el-table :data="authorities" size="small" border>
              <el-table-column prop="user" :label="$t('objects.authUser')" min-width="120" />
              <el-table-column prop="authority" :label="$t('objects.authType')" min-width="120" />
              <el-table-column prop="authorityType" :label="$t('objects.authDetail')" min-width="140" />
            </el-table>
            </RxSkeleton>
            <el-empty v-if="!authLoading && authorities.length === 0" :description="$t('objects.noAuth')" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'Objects' })
import { ref } from 'vue'
import QueryBar from '@/components/QueryBar.vue'
import { useUserStore } from '@/stores/user'
import {
  objectAuthorities,
  objectDetail,
  objectReferences,
  searchObjects,
  type ObjectAuthority,
  type ObjectDetail,
  type ObjectReference,
  type ObjectRow,
} from '@/api/object'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { formatSize } from '@/utils/format'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'

const userStore = useUserStore()
const library = ref('APP')
const type = ref<string | null>(null)
const types = ['PGM', 'SRVPGM', 'MODULE', 'FILE', '*DTAQ', 'MSGF']

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
  handleRefresh,
  handlePageChange,
  handleSizeChange,
} = useSmartQueryTable<ObjectRow>({
  fetchApi: (params) => searchObjects(params),
  enableCache: true,
  buildParams: (base) => ({
    library: library.value || undefined,
    type: type.value || undefined,
    keyword: base.keyword || undefined,
    current: base.current,
    size: base.size,
  }),
})

const drawerVisible = ref(false)
const detailRow = ref<ObjectRow | null>(null)
const detail = ref<ObjectDetail | null>(null)
const refTab = ref('IN')
const refIn = ref<ObjectReference[]>([])
const refOut = ref<ObjectReference[]>([])
const refLoading = ref(false)
const authorities = ref<ObjectAuthority[]>([])
const authLoading = ref(false)

const openDetail = async (row: ObjectRow) => {
  detailRow.value = row
  drawerVisible.value = true
  detail.value = null
  refIn.value = []
  refOut.value = []
  authorities.value = []
  try {
    detail.value = await objectDetail(row.OBJECT_LIBRARY, row.OBJECT_NAME)
  } catch {
    detail.value = null
  }
  await loadRefs()
}

const loadRefs = async () => {
  if (!detailRow.value) return
  refLoading.value = true
  try {
    const [inList, outList] = await Promise.all([
      objectReferences(detailRow.value.OBJECT_LIBRARY, detailRow.value.OBJECT_NAME, 'IN'),
      objectReferences(detailRow.value.OBJECT_LIBRARY, detailRow.value.OBJECT_NAME, 'OUT'),
    ])
    refIn.value = inList || []
    refOut.value = outList || []
  } catch {
    refIn.value = []
    refOut.value = []
  } finally {
    refLoading.value = false
  }
  await loadAuthorities()
}

const loadAuthorities = async () => {
  if (!detailRow.value) return
  authLoading.value = true
  try {
    authorities.value = await objectAuthorities(
      detailRow.value.OBJECT_LIBRARY,
      detailRow.value.OBJECT_NAME,
    )
  } catch {
    authorities.value = []
  } finally {
    authLoading.value = false
  }
}
</script>

<style scoped>
/* 公共样式（toolbar/mb16/mt16/count）已收敛至 src/styles/common.css */
</style>