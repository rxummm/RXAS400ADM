<template>
  <el-dialog v-model="visible" :title="$t('docs.uploadIfs')" width="560px">
    <el-form label-width="100px">
      <el-form-item :label="$t('docs.docTitle')">{{ doc?.title }}</el-form-item>
      <el-form-item :label="$t('docs.uploadSource')">
        <el-radio-group v-model="source">
          <el-radio v-if="!isBinary" value="content">{{ $t('docs.uploadFromContent') }}</el-radio>
          <el-radio value="file">{{ $t('docs.uploadFromFile') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="source === 'file'" :label="$t('ifs.selectFile')" required>
        <input type="file" @change="onFileChange" />
      </el-form-item>
      <el-form-item :label="$t('docs.uploadIfsTarget')" required>
        <el-input v-model="targetPath" class="w-full" :placeholder="$t('docs.uploadIfsPlaceholder')" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="uploading" @click="confirmUpload">{{ $t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { uploadIfsFile, writeIfsFile } from '@/api/ifs'
import { updateDoc, type DocItem } from '@/api/doc'

const props = defineProps<{
  modelValue: boolean
  doc: DocItem | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  /** 上传成功后回传实际路径（父组件持久化到行对象，保持详情抽屉即时可见） */
  (e: 'saved', path: string): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const { t } = useI18n()
const source = ref<'content' | 'file'>('content')
const targetPath = ref('')
const file = ref<File | null>(null)
const uploading = ref(false)

/** 文件型文档（PDF/图片）：内容以 IFS 文件为准，只能走「本地文件」上传 */
const isBinary = computed(() => props.doc?.docType === 'PDF' || props.doc?.docType === 'IMAGE')

/** 标题转 IFS 文件名：清洗非法字符，与后端上传路径校验一致 */
const sanitizeName = (name: string) =>
  name.replace(/[\\/:*?"<>|\s]+/g, '_').replace(/_+/g, '_').slice(0, 60) || 'doc'

// 每次打开按当前文档重置表单（上次路径优先）
watch(visible, (open) => {
  if (!open || !props.doc) return
  source.value = isBinary.value ? 'file' : 'content'
  file.value = null
  targetPath.value = props.doc.ifsPath || (isBinary.value
    ? `/QOpenSys/rxas400/document/${sanitizeName(props.doc.title)}`
    : `/QOpenSys/rxas400/document/${sanitizeName(props.doc.title)}.md`)
})

const onFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  file.value = input.files?.[0] || null
  if (file.value) {
    // 选择文件后自动带出目标路径（保留目录部分）
    const base = targetPath.value.substring(0, targetPath.value.lastIndexOf('/') + 1)
    targetPath.value = `${base}${file.value.name}`
  }
}

const confirmUpload = async () => {
  if (!props.doc) return
  const path = targetPath.value.trim()
  if (!path) {
    ElMessage.warning(t('docs.uploadIfsPathRequired'))
    return
  }
  if (source.value === 'file' && !file.value) {
    ElMessage.warning(t('docs.uploadFileRequired'))
    return
  }
  uploading.value = true
  try {
    let savedPath: string
    if (source.value === 'file' && file.value) {
      const res = await uploadIfsFile(path, file.value)
      savedPath = res.path
    } else {
      const res = await writeIfsFile(path, props.doc.content || '')
      savedPath = res.path
    }
    // 持久化 ifsPath 到 rx_doc（V32），重启后详情仍显示；失败不阻塞（已上传成功）
    try {
      await updateDoc(props.doc.id, { title: props.doc.title, content: props.doc.content, ifsPath: savedPath })
    } catch {
      /* 持久化失败不阻塞 */
    }
    visible.value = false
    ElMessage.success(`${t('docs.uploaded')}: ${savedPath}`)
    emit('saved', savedPath)
  } catch {
    /* 拦截器已提示 */
  } finally {
    uploading.value = false
  }
}
</script>
