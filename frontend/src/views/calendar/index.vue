<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button-group>
        <el-button @click="shiftMonth(-1)">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <el-button @click="goToday">{{ $t('calendar.today') }}</el-button>
        <el-button @click="shiftMonth(1)">
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </el-button-group>
      <span class="month-title">{{ monthTitle }}</span>
      <div class="flex-1" />
      <el-button type="primary" @click="openEventDialog(null, todayStr())">
        <el-icon><Plus /></el-icon> {{ $t('calendar.addEvent') }}
      </el-button>
    </div>

    <div class="table-wrapper calendar-wrap">
      <div class="weekdays">
        <div v-for="d in weekdays" :key="d">{{ d }}</div>
      </div>
      <div class="grid">
        <div
          v-for="cell in cells"
          :key="cell.dateStr"
          class="cell"
          :class="{
            'is-outside': cell.outside,
            'is-today': cell.isToday,
          }"
          @click="openDayDialog(cell)"
        >
          <div class="cell-header">
            <span class="cell-day" :class="{ 'today-dot': cell.isToday }">{{ cell.day }}</span>
          </div>
          <div class="cell-events">
            <div
              v-for="evt in cell.events.slice(0, 3)"
              :key="evt.id"
              class="cell-event"
              :style="{ borderLeftColor: evt.color || 'var(--color-primary)' }"
              @click.stop="openEventDialog(evt, cell.dateStr)"
            >
              <span v-if="!evt.isAllDay && evt.startTime" class="evt-time">{{ evt.startTime.slice(0, 5) }}</span>
              <span class="evt-title">{{ evt.title }}</span>
            </div>
            <div v-if="cell.events.length > 3" class="cell-more">+{{ cell.events.length - 3 }} {{ $t('calendar.more') }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 某天事件列表 -->
    <el-dialog v-model="dayDialogVisible" :title="selectedDayTitle" width="480px" :close-on-click-modal="false">
      <div v-if="selectedDayEvents.length" class="day-event-list">
        <div v-for="evt in selectedDayEvents" :key="evt.id" class="day-event-item">
          <span class="event-color-bar" :style="{ background: evt.color || 'var(--color-primary)' }" />
          <div class="event-info">
            <div class="event-title">{{ evt.title }}</div>
            <div class="event-meta">
              <span v-if="!evt.isAllDay && evt.startTime" class="event-time">
                {{ evt.startTime.slice(0, 5) }}{{ evt.endTime ? ' - ' + evt.endTime.slice(0, 5) : '' }}
              </span>
              <el-tag size="small" effect="plain">{{ typeLabel(evt.eventType) }}</el-tag>
            </div>
          </div>
          <div class="event-actions">
            <el-button text size="small" @click="openEventDialog(evt, '')">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button text size="small" type="danger" @click="onDelete(evt.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-else :description="$t('calendar.noEvents')" />
      <template #footer>
        <el-button type="primary" @click="openEventDialog(null, selectedDay)">
          <el-icon><Plus /></el-icon> {{ $t('calendar.addEvent') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 新建/编辑事件 -->
    <EventFormDialog v-model="eventDialogVisible" :event="editingEvent" :date="editingDate" @saved="loadMonth" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, ArrowRight, Delete, Edit, Plus } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchMonthEvents,
  deleteEvent,
  type CalendarEvent,
} from '@/api/calendar'
import EventFormDialog from './EventFormDialog.vue'

defineOptions({ name: 'CalendarIndex' })

const { t, tm } = useI18n()

// 数组型翻译用 tm()（t() 只返回字符串）
const weekdays = tm('calendar.weekdays') as string[]
const typeOptions = computed(() => [
  { value: 'meeting', label: t('calendar.typeMeeting') },
  { value: 'task', label: t('calendar.typeTask') },
  { value: 'reminder', label: t('calendar.typeReminder') },
  { value: 'other', label: t('calendar.typeOther') },
])
const typeLabel = (v?: string) =>
  typeOptions.value.find((o) => o.value === v)?.label || t('calendar.typeOther')

// ==================== 事件表单 ====================
const eventDialogVisible = ref(false)
const editingEvent = ref<CalendarEvent | null>(null)
const editingDate = ref('')

function openEventDialog(evt: CalendarEvent | null, date: string) {
  editingEvent.value = evt
  editingDate.value = date
  eventDialogVisible.value = true
}

// ==================== 月网格 ====================
const today = new Date()
const viewYear = ref(today.getFullYear())
const viewMonth = ref(today.getMonth() + 1)
const eventsByDate = ref<Record<string, CalendarEvent[]>>({})

const monthTitle = computed(() => t('calendar.yearMonth', { y: viewYear.value, m: viewMonth.value }))

interface Cell {
  dateStr: string
  day: number
  outside: boolean
  isToday: boolean
  events: CalendarEvent[]
}

const cells = computed<Cell[]>(() => {
  const first = new Date(viewYear.value, viewMonth.value - 1, 1)
  const startOffset = first.getDay() // 周日=0
  const start = new Date(viewYear.value, viewMonth.value - 1, 1 - startOffset)
  const result: Cell[] = []
  for (let i = 0; i < 42; i++) {
    const d = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i)
    const dateStr = fmt(d)
    result.push({
      dateStr,
      day: d.getDate(),
      outside: d.getMonth() !== viewMonth.value - 1,
      isToday: fmt(d) === fmt(today),
      events: eventsByDate.value[dateStr] || [],
    })
  }
  return result
})

function fmt(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}
const todayStr = () => fmt(today)

async function loadMonth() {
  const list = (await fetchMonthEvents(viewYear.value, viewMonth.value)) as CalendarEvent[]
  const map: Record<string, CalendarEvent[]> = {}
  list.forEach((evt) => {
    const key = String(evt.eventDate)
    ;(map[key] = map[key] || []).push(evt)
  })
  eventsByDate.value = map
}

const shiftMonth = (delta: number) => {
  const d = new Date(viewYear.value, viewMonth.value - 1 + delta, 1)
  viewYear.value = d.getFullYear()
  viewMonth.value = d.getMonth() + 1
}

const goToday = () => {
  viewYear.value = today.getFullYear()
  viewMonth.value = today.getMonth() + 1
}

// ==================== 某天事件弹窗 ====================
const dayDialogVisible = ref(false)
const selectedDay = ref('')
const selectedDayTitle = computed(() => `${selectedDay.value} · ${t('calendar.events')}`)
const selectedDayEvents = computed(() => eventsByDate.value[selectedDay.value] || [])

function openDayDialog(cell: Cell) {
  selectedDay.value = cell.dateStr
  dayDialogVisible.value = true
}

async function onDelete(id?: number) {
  if (!id) return
  try {
    await ElMessageBox.confirm(t('calendar.deleteConfirm'), t('common.tip'), { type: 'warning' })
    await deleteEvent(id)
    ElMessage.success(t('common.deleteSuccess'))
    loadMonth()
  } catch {
    /* cancelled */
  }
}

onMounted(loadMonth)
</script>

<style scoped>
.month-title {
  font-size: 16px;
  font-weight: 600;
  margin-left: 12px;
  color: var(--text-primary);
}
.calendar-wrap {
  padding: 12px;
}
.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-regular);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-light);
}
.grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-top: 4px;
}
.cell {
  min-height: 96px;
  border: 1px solid var(--border-lighter);
  border-radius: 4px;
  padding: 4px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
  overflow: hidden;
}
.cell:hover {
  border-color: var(--color-primary);
  background: var(--bg-hover);
}
.cell.is-outside {
  opacity: 0.45;
}
.cell.is-today {
  border-color: var(--color-primary);
}
.cell-header {
  display: flex;
  justify-content: flex-end;
}
.cell-day {
  font-size: 12px;
  color: var(--text-regular);
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}
.today-dot {
  background: var(--color-primary);
  color: var(--bg-container);
  font-weight: 600;
}
.cell-events {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 2px;
}
.cell-event {
  font-size: 12px;
  padding: 1px 4px;
  border-left: 3px solid var(--color-primary);
  background: var(--bg-hover);
  border-radius: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text-regular);
}
.cell-event:hover {
  color: var(--color-primary);
}
.evt-time {
  color: var(--text-secondary);
  margin-right: 4px;
  font-size: 11px;
}
.evt-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cell-more {
  font-size: 11px;
  color: var(--text-secondary);
  padding-left: 4px;
}
.day-event-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow: auto;
}
.day-event-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid var(--border-lighter);
  border-radius: 6px;
}
.event-color-bar {
  width: 4px;
  align-self: stretch;
  border-radius: 2px;
  flex-shrink: 0;
}
.event-info {
  flex: 1;
  min-width: 0;
}
.event-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}
.event-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}
.event-time {
  font-size: 12px;
  color: var(--text-secondary);
}
.event-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}
</style>