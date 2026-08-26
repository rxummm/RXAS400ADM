<template>
  <el-drawer v-model="visible" :title="$t('docs.history')" size="480px">
    <el-timeline v-if="versions.length">
      <el-timeline-item v-for="v in versions" :key="v.id" :timestamp="v.createdTime" placement="top">
        <el-card shadow="never">
          <div class="version-row">
            <span><b>v{{ v.version }}</b> · {{ v.operator }}</span>
            <span>
              <el-button size="small" link type="primary" :loading="loadingPreviewId === v.id" @click="openPreview(v)">{{ $t('docs.preview') }}</el-button>
              <el-button size="small" link type="warning" :loading="removeLoading_doRollback === v.id" @click="doRollback(v)">{{ $t('docs.rollback') }}</el-button>
            </span>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else :description="$t('docs.noVersions')" />
    <template v-if="preview">
      <h4 class="section">{{ $t('docs.preview') }} · v{{ preview.version }}</h4>
      <div v-loading="loadingPreviewId === preview.id">
        <DocRenderer v-if="previewContent" :content="previewContent" :doc-type="docType" />
        <el-empty v-else-if="loadingPreviewId !== preview.id" :description="$t('common.noData')" />
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { docVersions, fetchDocVersionContent, rollbackDoc, type DocType, type DocVersion } from '@/api/doc'
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
// P12 版本正文懒加载：列表接口已瘦身不含 content，点击预览时按版本 id 单点拉取
const previewContent = ref('')
const loadingPreviewId = ref<number | null>(null)
// 版本快照不可变，正文按版本 id 缓存避免重复请求
const contentCache = new Map<number, string>()

// 打开时按当前文档拉取版本历史
watch(visible, async (open) => {
  if (!open || props.docId == null) return
  preview.value = null
  previewContent.value = ''
  try {
    versions.value = await docVersions(props.docId)
  } catch {
    versions.value = []
  }
})

const openPreview = async (v: DocVersion) => {
  if (loadingPreviewId.value != null) return
  preview.value = v
  const cached = contentCache.get(v.id)
  if (cached != null) {
    previewContent.value = cached
    return
  }
  previewContent.value = ''
  loadingPreviewId.value = v.id
  try {
    const content = await fetchDocVersionContent(v.id)
    previewContent.value = content
    contentCache.set(v.id, content)
  } catch {
    // 失败留空正文（渲染器对空内容有兜底展示），错误提示由 request 拦截器统一弹出
    previewContent.value = ''
  } finally {
    loadingPreviewId.value = null
  }
}

const removeLoading_doRollback = ref<number | null>(null)
const doRollback = async (v: DocVersion) => {
  if (props.docId == null) return
  try {
    await ElMessageBox.confirm(t('docs.rollbackConfirm', { version: v.version }), t('common.confirm'), { type: 'warning' })
  } catch {
    return
  }
  removeLoading_doRollback.value = v.id
  try {
    await rollbackDoc(props.docId, v.version)
    ElMessage.success(t('docs.rollbacked'))
    emit('changed')
    visible.value = false
  } finally {
    removeLoading_doRollback.value = null
  }
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