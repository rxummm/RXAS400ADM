<template>
  <el-dialog v-model="visible" :title="isEdit ? $t('common.edit') : $t('calendar.addEvent')" width="520px" :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item :label="$t('calendar.title')" prop="title">
        <el-input v-model="form.title" maxlength="128" />
      </el-form-item>
      <el-form-item :label="$t('calendar.date')" prop="eventDate">
        <el-date-picker v-model="form.eventDate" type="date" value-format="YYYY-MM-DD" class="w-full" />
      </el-form-item>
      <el-form-item :label="$t('calendar.allDay')">
        <el-switch v-model="form.isAllDay" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item v-if="form.isAllDay !== 1" :label="$t('calendar.time')">
        <el-time-picker
          v-model="form.timeRange"
          is-range
          format="HH:mm"
          value-format="HH:mm:ss"
          :start-placeholder="$t('calendar.startTime')"
          :end-placeholder="$t('calendar.endTime')"
          class="w-full"
        />
      </el-form-item>
      <el-form-item :label="$t('calendar.type')">
        <el-select v-model="form.eventType" class="w-full">
          <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('calendar.priority')">
        <el-radio-group v-model="form.priority">
          <el-radio :value="1">{{ $t('calendar.priorityLow') }}</el-radio>
          <el-radio :value="2">{{ $t('calendar.priorityMid') }}</el-radio>
          <el-radio :value="3">{{ $t('calendar.priorityHigh') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="$t('calendar.color')">
        <div class="color-picker">
          <span
            v-for="c in colorPalette"
            :key="c"
            class="color-dot"
            :class="{ active: form.color === c }"
            :style="{ background: c }"
            @click="form.color = c"
          />
        </div>
      </el-form-item>
      <el-form-item :label="$t('calendar.description')">
        <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="onSave">{{ $t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { createEvent, updateEvent, type CalendarEvent } from '@/api/calendar'

const props = defineProps<{
  modelValue: boolean
  event: CalendarEvent | null
  date: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const isEdit = computed(() => !!props.event)

const { t } = useI18n()
const typeOptions = computed(() => [
  { value: 'meeting', label: t('calendar.typeMeeting') },
  { value: 'task', label: t('calendar.typeTask') },
  { value: 'reminder', label: t('calendar.typeReminder') },
  { value: 'other', label: t('calendar.typeOther') },
])

const colorPalette = ['#1677ff', '#67c23a', '#e6a23c', '#f56c6c', '#9c27b0', '#00bcd4', '#909399']

const formRef = ref()
const submitting = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  title: '',
  eventDate: '' as string,
  isAllDay: 1,
  timeRange: null as [string, string] | null,
  eventType: 'other',
  priority: 1,
  color: '',
  description: '',
})

const rules = {
  title: [{ required: true, message: () => t('calendar.titleRequired'), trigger: 'blur' }],
  eventDate: [{ required: true, message: () => t('calendar.dateRequired'), trigger: 'change' }],
}

function init(evt: CalendarEvent | null, date: string) {
  Object.assign(form, {
    id: evt?.id,
    title: evt?.title || '',
    eventDate: evt?.eventDate || date,
    isAllDay: evt?.isAllDay ?? 1,
    timeRange: evt?.startTime && evt.endTime ? [evt.startTime, evt.endTime] : null,
    eventType: evt?.eventType || 'other',
    priority: evt?.priority || 1,
    color: evt?.color || colorPalette[0],
    description: evt?.description || '',
  })
}

// 打开弹窗时用当前事件/日期回填表单（父组件只传 props，不显式调用 init）
watch(
  () => props.modelValue,
  (open) => {
    if (open) init(props.event, props.date)
  },
)

defineExpose({ init })

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = {
      title: form.title,
      eventDate: form.eventDate,
      isAllDay: form.isAllDay,
      startTime: form.isAllDay === 1 ? null : form.timeRange?.[0] || null,
      endTime: form.isAllDay === 1 ? null : form.timeRange?.[1] || null,
      eventType: form.eventType,
      priority: form.priority,
      color: form.color,
      description: form.description,
    }
    if (isEdit.value && form.id) {
      await updateEvent(form.id, payload)
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createEvent(payload)
      ElMessage.success(t('common.addSuccess'))
    }
    visible.value = false
    emit('saved')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.color-picker {
  display: flex;
  gap: 8px;
}
.color-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: transform 0.15s;
}
.color-dot:hover {
  transform: scale(1.15);
}
.color-dot.active {
  border-color: var(--text-primary);
  transform: scale(1.1);
}
</style>