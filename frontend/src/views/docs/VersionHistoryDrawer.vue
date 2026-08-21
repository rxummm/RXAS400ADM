<template>
  <el-drawer v-model="visible" :title="$t('docs.history')" size="480px">
    <el-timeline v-if="versions.length">
      <el-timeline-item v-for="v in versions" :key="v.id" :timestamp="v.createdTime" placement="top">
        <el-card shadow="never">
          <div class="version-row">
            <span><b>v{{ v.version }}</b> · {{ v.operator }}</span>
            <span>
              <el-button size="small" link type="primary" @click="preview = v">{{ $t('docs.preview') }}</el-button>
              <el-button size="small" link type="warning" @click="doRollback(v)">{{ $t('docs.rollback') }}</el-button>
            </span>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else :description="$t('docs.noVersions')" />
    <template v-if="preview">
      <h4 class="section">{{ $t('docs.preview') }} · v{{ preview.version }}</h4>
      <DocRenderer :content="preview.content || ''" :doc-type="docType" />
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { docVersions, rollbackDoc, type DocType, type DocVersion } from '@/api/doc'
import DocRenderer from './DocRenderer.vue'

const props = defineProps<{
  modelValue: boolean
  docId: number | null
  /** 文档类型（版本快照本身不带类型，按当前文档类型渲染） */
  docType?: DocType
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'changed'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const { t } = useI18n()
const versions = ref<DocVersion[]>([])
const preview = ref<DocVersion | null>(null)

// 打开时按当前文档拉取版本历史
watch(visible, async (open) => {
  if (!open || props.docId == null) return
  preview.value = null
  try {
    versions.value = await docVersions(props.docId)
  } catch {
    versions.value = []
  }
})

const doRollback = async (v: DocVersion) => {
  if (props.docId == null) return
  await ElMessageBox.confirm(t('docs.rollbackConfirm', { version: v.version }), t('common.confirm'), { type: 'warning' })
  await rollbackDoc(props.docId, v.version)
  ElMessage.success(t('docs.rollbacked'))
  emit('changed')
  visible.value = false
}
</script>

<style scoped>
.version-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
</style>