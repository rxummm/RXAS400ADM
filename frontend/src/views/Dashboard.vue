<template>
  <div>
    <div class="search-bar">
      <div class="dash-title">{{ $t('dashboard.widgets') }}</div>
      <div class="flex-1" />
      <el-button @click="openCustomize">
        <el-icon><Setting /></el-icon> {{ $t('dashboard.customize') }}
      </el-button>
    </div>

    <el-row v-if="visibleWidgets.length > 0" :gutter="16">
      <el-col v-for="card in visibleWidgets" :key="card.key" :xs="12" :sm="12" :md="8" :lg="6" :xl="6">
        <el-card shadow="never">
          <div class="stat">
            <div class="stat-icon" :style="{ background: card.color }">
              <el-icon :size="26" class="stat-icon-text"><component :is="card.icon" /></el-icon>
            </div>
            <div>
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-title">{{ $t(card.title) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="widgetEnabled('overview')" shadow="never" class="mt16">
      <template #header>{{ $t('dashboard.overview') }}</template>
      <RxSkeleton type="table" :rows="5" :loading="loading">
        <el-table :data="pagedSystems" size="small" border>
        <el-table-column :label="$t('dashboard.name')" prop="name" width="140" />
        <el-table-column :label="$t('dashboard.host')" prop="host" width="160" />
        <el-table-column :label="$t('dashboard.environment')" prop="environment" width="100">
          <template #default="{ row }">
            <el-tag :type="envType(row.environment)">{{ row.environment }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('dashboard.level')" prop="criticalLevel" width="120" />
        <el-table-column :label="$t('dashboard.status')" prop="status" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ONLINE' ? 'success' : 'danger'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('dashboard.description')" prop="description" />
      </el-table>
      </RxSkeleton>
      <AppPagination :total="systems.length" v-model:current="overviewCurrent" v-model:size="overviewSize" @change="() => {}" @size-change="onOverviewSizeChange" />
    </el-card>

    <el-card v-if="widgetEnabled('favoriteScripts')" shadow="never" class="mt16">
      <template #header>
        <div class="flex-row-center">
          <span>{{ $t('dashboard.favoriteScripts') }}</span>
          <el-button link type="primary" size="small" class="ml8" @click="$router.push('/scripts')">{{ $t('common.viewAll') }}</el-button>
        </div>
      </template>
      <el-table :data="favoriteScripts" size="small" border>
        <el-table-column prop="name" :label="$t('scripts.name')" min-width="120" />
        <el-table-column prop="command" :label="$t('scripts.command')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="runCount" :label="$t('scripts.runCount')" width="80" />
      </el-table>
      <el-empty v-if="favoriteScripts.length === 0" :description="$t('dashboard.noFavoriteScripts')" />
    </el-card>

    <el-dialog v-model="customizeVisible" :title="$t('dashboard.customize')" width="var(--rx-dialog-xs)" :close-on-click-modal="false">
      <el-alert type="info" :title="$t('dashboard.customizeHint')" :closable="false" class="mb16" />
      <div v-for="w in widgetOptions" :key="w.key" class="widget-toggle">
        <span>{{ $t(w.label) }}</span>
        <el-switch :model-value="isWidgetOn(w.key)" @change="(val: string | number | boolean) => toggleWidget(w.key, Boolean(val))" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Setting } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { fetchSystems, type IbmiSystem } from '@/api/as400'
import { getDashboardWidgets, updateDashboardWidget } from '@/api/dashboardWidget'
import { listScripts, type CommandScript } from '@/api/script'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t } = useI18n()

const loading = ref(false)
const systems = ref<IbmiSystem[]>([])
const prefs = ref<Record<string, number>>({})
const customizeVisible = ref(false)
const overviewCurrent = ref(1)
const overviewSize = ref(10)

// 常用命令
const favoriteScripts = ref<CommandScript[]>([])

const pagedSystems = computed(() =>
  systems.value.slice((overviewCurrent.value - 1) * overviewSize.value, overviewCurrent.value * overviewSize.value),
)

const onOverviewSizeChange = () => {
  overviewCurrent.value = 1
}

const widgetOptions = [
  { key: 'systems', label: 'dashboard.systems' },
  { key: 'online', label: 'dashboard.online' },
  { key: 'production', label: 'dashboard.production' },
  { key: 'alerts', label: 'dashboard.alerts' },
  { key: 'overview', label: 'dashboard.overview' },
  { key: 'favoriteScripts', label: 'dashboard.favoriteScripts' },
]

const cards = computed(() => [
  { key: 'systems', title: 'dashboard.systems', value: systems.value.length, icon: 'Cpu', color: 'var(--color-primary)' },
  {
    key: 'online',
    title: 'dashboard.online',
    value: systems.value.filter((s) => s.status === 'ONLINE').length,
    icon: 'Connection',
    color: 'var(--color-success)',
  },
  {
    key: 'production',
    title: 'dashboard.production',
    value: systems.value.filter((s) => s.environment === 'PROD').length,
    icon: 'Promotion',
    color: 'var(--color-warning)',
  },
  { key: 'alerts', title: 'dashboard.alerts', value: 0, icon: 'Warning', color: 'var(--color-danger)' },
])

const visibleWidgets = computed(() => cards.value.filter((c) => isWidgetOn(c.key)))

function isWidgetOn(key: string) {
  return prefs.value[key] === undefined || prefs.value[key] === 1
}
function widgetEnabled(key: string) {
  return isWidgetOn(key)
}

async function loadPrefs() {
  try {
    const list = (await getDashboardWidgets()) || []
    prefs.value = list.reduce(
      (acc, w) => ({ ...acc, [w.widgetKey]: w.enabled ?? 1 }),
      {} as Record<string, number>,
    )
  } catch {
    /* interceptor 已提示错误 */
  }
}

async function toggleWidget(key: string, val: boolean) {
  try {
    await updateDashboardWidget(key, val)
    prefs.value[key] = val ? 1 : 0
    ElMessage.success(t('dashboard.customizeSaved'))
  } catch {
    /* request interceptor shows error */
  }
}

function openCustomize() {
  customizeVisible.value = true
}

const envType = (env: string): 'danger' | 'warning' | 'primary' | 'info' =>
  ({ PROD: 'danger', TEST: 'warning', DEV: 'primary', DR: 'info' } as Record<string, 'danger' | 'warning' | 'primary' | 'info'>)[env] || 'info'

const load = async () => {
  loading.value = true
  try {
    systems.value = await fetchSystems()
  } finally {
    loading.value = false
  }
}

async function loadFavoriteScripts() {
  try {
    favoriteScripts.value = await listScripts({ favorite: true })
  } catch {
    /* ignored */
  }
}

onMounted(() => {
  load()
  loadPrefs()
  loadFavoriteScripts()
})
</script>

<style scoped>
.dash-title {
  font-size: 16px;
  font-weight: 600;
}
.stat {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
}
.stat-icon-text {
  color: var(--bg-container);
}
.stat-title {
  color: var(--text-secondary);
  font-size: 13px;
}
.widget-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 4px;
  border-bottom: 1px solid var(--border-light);
}
</style>