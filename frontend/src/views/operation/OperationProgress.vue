<template>
  <div class="operation-progress">
    <div class="operation-progress__header">
      <span class="operation-progress__type">{{ localOperation.operationType }}</span>
      <el-tag :type="statusType(localOperation.status)" size="small">
        {{ localOperation.status }}
      </el-tag>
    </div>

    <div class="operation-progress__steps">
      <div
        v-for="(step, index) in steps"
        :key="index"
        class="operation-progress__step"
        :class="{
          'operation-progress__step--active': step.code === localOperation.currentStep,
          'operation-progress__step--done': step.done,
          'operation-progress__step--failed': step.failed
        }"
      >
        <div class="operation-progress__step-icon">
          <el-icon v-if="step.done"><Check /></el-icon>
          <el-icon v-else-if="step.failed"><Close /></el-icon>
          <span v-else>{{ index + 1 }}</span>
        </div>
        <div class="operation-progress__step-label">{{ step.label }}</div>
      </div>
    </div>

    <div v-if="localOperation.errorCode" class="operation-progress__error">
      <span class="text-danger">{{ localOperation.errorCode }}</span>
      <span class="text-muted ml4">{{ localOperation.errorMessage }}</span>
    </div>

    <div v-if="localOperation.status === 'RUNNING'" class="operation-progress__actions">
      <el-button size="small" type="danger" @click="$emit('cancel')">
        {{ $t('operation.cancel') }}
      </el-button>
    </div>

    <div v-if="localOperation.status === 'FAILED' && localOperation.retryCount < localOperation.maxRetry" class="operation-progress__actions">
      <el-button size="small" type="warning" @click="$emit('retry')">
        {{ $t('operation.retry') }} ({{ localOperation.retryCount }}/{{ localOperation.maxRetry }})
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { Check, Close } from '@element-plus/icons-vue'
import type { OperationVO } from '@/api/operation'
import { useStompClient } from '@/composables/useStompClient'
import type { IMessage } from '@stomp/stompjs'

const props = defineProps<{
  operation: OperationVO
  stepLabels?: Record<string, string>
}>()

const emit = defineEmits<{
  cancel: []
  retry: []
  updated: [op: OperationVO]
}>()

const { connect, disconnect } = useStompClient()
const localOperation = ref<OperationVO>({ ...props.operation })

watch(() => props.operation, (val) => {
  localOperation.value = { ...val }
}, { immediate: true, deep: true })

const steps = computed(() => {
  const op = localOperation.value
  const allSteps = op.steps || []
  const currentIdx = allSteps.indexOf(op.currentStep || '')
  const isFailed = op.status === 'FAILED'

  return allSteps.map((step, idx) => ({
    code: step,
    label: props.stepLabels?.[step] || step,
    done: idx < currentIdx || op.status === 'SUCCESS',
    failed: isFailed && idx === currentIdx
  }))
})

onMounted(() => {
  if (!localOperation.value.id) return
  connect((client) => {
    client.subscribe('/topic/operations/' + localOperation.value.id, (message: IMessage) => {
      try {
        const data = JSON.parse(message.body)
        if (data.stepCode) {
          // 更新当前步骤状态
          localOperation.value = {
            ...localOperation.value,
            currentStep: data.stepCode,
            status: data.operationStatus || localOperation.value.status
          }
          // 如果操作已完成，停止轮询并通知父组件
          if (['SUCCESS', 'FAILED', 'CANCELLED'].includes(data.operationStatus)) {
            emit('updated', localOperation.value)
          }
        }
      } catch {
        // JSON 解析失败忽略
      }
    })
  })
})

onUnmounted(() => {
  disconnect()
})

function statusType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    SUCCESS: 'success',
    RUNNING: 'info',
    FAILED: 'danger',
    CANCELLED: 'info',
    RETRYING: 'warning',
    REQUESTED: 'info'
  }
  return map[status] || 'info'
}
</script>

<style scoped>
.operation-progress {
  padding: 16px;
}
.operation-progress__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.operation-progress__type {
  font-weight: 600;
  font-size: 14px;
}
.operation-progress__steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}
.operation-progress__step {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  background: var(--bg-page);
  transition: all 0.2s;
}
.operation-progress__step--active {
  background: var(--color-primary-light-9);
  border-left: 3px solid var(--color-primary);
}
.operation-progress__step--done {
  opacity: 0.6;
}
.operation-progress__step--done .operation-progress__step-icon {
  color: var(--color-success);
}
.operation-progress__step--failed .operation-progress__step-icon {
  color: var(--color-danger);
}
.operation-progress__step-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  background: var(--bg-page);
  border: 1px solid var(--border-color);
}
.operation-progress__step-label {
  font-size: 13px;
}
.operation-progress__error {
  padding: 8px 12px;
  border-radius: 6px;
  background: var(--color-danger-light-9);
  margin-bottom: 12px;
  font-size: 12px;
}
.operation-progress__actions {
  display: flex;
  justify-content: flex-end;
}
</style>