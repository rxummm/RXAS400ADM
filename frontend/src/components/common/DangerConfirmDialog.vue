<template>
  <el-dialog v-model="visible" :title="$t('common.dangerConfirm')" width="400px" :close-on-click-modal="false">
    <el-alert type="warning" :closable="false" show-icon>
      <template #title>
        <div class="danger-confirm-content">
          <div class="danger-confirm-icon">⚠️</div>
          <div class="danger-confirm-message">{{ message }}</div>
          <div v-if="detail" class="danger-confirm-detail">{{ detail }}</div>
        </div>
      </template>
    </el-alert>
    <template #footer>
      <el-button @click="handleCancel">{{ $t('common.cancel') }}</el-button>
      <el-button type="danger" @click="handleConfirm">{{ $t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue: boolean
  message: string
  detail?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const visible = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const handleConfirm = () => {
  visible.value = false
  emit('confirm')
}

const handleCancel = () => {
  visible.value = false
  emit('cancel')
}
</script>

<style scoped>
.danger-confirm-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.danger-confirm-icon {
  font-size: 24px;
}

.danger-confirm-message {
  font-size: 14px;
  line-height: 1.5;
}

.danger-confirm-detail {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 8px;
  padding: 8px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}
</style>
