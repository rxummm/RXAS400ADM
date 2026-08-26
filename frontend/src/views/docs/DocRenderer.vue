<template>
  <div class="doc-renderer">
    <template v-if="isTextType">
      <el-empty v-if="!content" :description="t('docs.emptyContent')" />
      <!-- eslint-disable-next-line vue/no-v-html -- 内容已经 DOMPurify 消毒（markdown/html 渲染），禁止绕过消毒 -->
      <div v-else-if="renderHtml" class="doc-html" v-html="renderHtml"></div>
      <pre v-else class="content">{{ content }}</pre>
    </template>
    <template v-else-if="isBinaryType">
      <iframe v-if="docType === 'PDF' && fileUrl" class="doc-iframe" :src="fileUrl" :title="t('docs.preview')"></iframe>
      <img v-else-if="docType === 'IMAGE' && fileUrl" class="doc-image" :src="fileUrl" :alt="t('docs.preview')" />
      <el-empty v-else :description="t('docs.fileUnavailable')" />
    </template>
    <el-empty v-else :description="t('docs.emptyContent')" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { useI18n } from 'vue-i18n'
import type { DocType } from '@/api/doc'

const props = defineProps<{
  content: string
  docType?: DocType
  /** PDF/图片 等文件型文档的 IFS 预览地址（对象 URL） */
  fileUrl?: string
}>()

const { t } = useI18n()

marked.setOptions({ gfm: true, breaks: true })

const isTextType = computed(() => !props.docType || ['MARKDOWN', 'TEXT', 'HTML'].includes(props.docType))
const isBinaryType = computed(() => props.docType === 'PDF' || props.docType === 'IMAGE')

/** 文本型文档渲染结果：MARKDOWN→marked 渲染、HTML→原样、TEXT→null（走 <pre>）；均经 DOMPurify 消毒 */
const renderHtml = computed(() => {
  if (!props.content) return ''
  if (props.docType === 'HTML') {
    return DOMPurify.sanitize(props.content)
  }
  if (props.docType === 'MARKDOWN') {
    const raw = marked.parse(props.content, { async: false }) as string
    return DOMPurify.sanitize(raw)
  }
  return null
})
</script>

<style scoped>
.doc-renderer {
  width: 100%;
}

.content {
  white-space: pre-wrap;
  word-break: break-all;
  background: var(--bg-hover);
  border-radius: 4px;
  padding: 12px;
  max-height: 480px;
  overflow: auto;
  font-size: 13px;
  margin: 0;
}

.doc-html {
  max-height: 480px;
  overflow: auto;
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
}

.doc-html :deep(pre) {
  background: var(--bg-hover);
  border-radius: 4px;
  padding: 10px;
  overflow: auto;
}

.doc-html :deep(code) {
  font-family: monospace;
}

.doc-iframe {
  width: 100%;
  height: 560px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background: var(--bg-hover);
}

.doc-image {
  max-width: 100%;
  max-height: 560px;
  border-radius: 4px;
  display: block;
}
</style>