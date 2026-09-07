<template>
  <div class="page-container">
    <el-card>
      <el-form :model="form" label-width="80px" ref="formRef" :rules="rules" v-loading="sending">
        <el-form-item :label="$t('compose.sender')" prop="sender">
          <div class="recipients-row">
            <el-input v-model="form.sender" :placeholder="$t('compose.senderHint')" class="recipients-input" />
            <el-select v-model="selectedSender" :placeholder="$t('compose.selectSender')" clearable
              @change="handleSenderSelect" class="recipients-group-select">
              <el-option v-if="senders.length === 0" disabled :label="$t('common.noData')" value="" />
              <el-option v-for="s in senders" :key="s" :label="s" :value="s" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item :label="$t('compose.recipients')" prop="recipients">
          <div class="recipients-row">
            <el-input v-model="form.recipients" :placeholder="$t('compose.recipientsHint')" class="recipients-input" />
            <el-select v-model="selectedGroup" :placeholder="$t('compose.fromGroup')" clearable
              @change="handleGroupSelect" class="recipients-group-select">
              <el-option v-if="groups.length === 0" disabled :label="$t('common.noData')" value="" />
              <el-option v-for="g in groups" :key="g.id" :label="g.groupName + ' (' + (g.memberCount || 0) + ')'" :value="g.id" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item :label="$t('compose.subject')" prop="subject">
          <el-input v-model="form.subject" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('compose.body')" prop="text" class="editor-form-item">
          <div class="editor-wrapper">
            <Toolbar :editor="editorRef" :defaultConfig="toolbarConfig" class="editor-toolbar" />
            <Editor
              v-model="form.htmlContent"
              :defaultConfig="editorConfig"
              class="editor-content"
              @onCreated="handleEditorCreated"
            />
          </div>
        </el-form-item>
        <el-form-item :label="$t('compose.attachments')">
          <div class="w-full">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="5"
              :on-change="handleFileChange"
              :on-exceed="handleExceed"
              :on-remove="handleFileRemove"
              :file-list="fileList"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.gif,.txt,.csv"
            >
              <el-button size="small" type="primary">{{ $t('compose.addAttachment') }}</el-button>
              <span class="hint ml8">{{ $t('compose.attachmentHint') }}</span>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item :label="$t('compose.priority')">
          <el-select v-model="form.priority" class="w-200">
            <el-option :label="$t('compose.normal')" value="NORMAL" />
            <el-option :label="$t('compose.high')" value="HIGH" />
            <el-option :label="$t('compose.low')" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSend" :loading="sending">{{ $t('compose.send') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef, onBeforeUnmount, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadFile, UploadInstance } from 'element-plus'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IToolbarConfig, IEditorConfig } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { listAllEmailGroups, listGroupMembers, sendEmail, listSenders } from '@/api/email'
import type { EmailGroup } from '@/api/email'

defineOptions({ name: 'Compose' })

const { t } = useI18n()
const sending = ref(false)
const formRef = ref<FormInstance>()
const uploadRef = ref<UploadInstance>()
const groups = ref<EmailGroup[]>([])
const selectedGroup = ref<number | null>(null)
const senders = ref<string[]>([])
const selectedSender = ref<string>('')
const fileList = ref<UploadFile[]>([])

const editorRef = shallowRef<IDomEditor>()

const toolbarConfig: Partial<IToolbarConfig> = {
  toolbarKeys: [
    'bold', 'italic', 'underline', 'through', '|',
    'color', 'bgColor', '|',
    'fontSize', 'fontFamily', '|',
    'justifyLeft', 'justifyCenter', 'justifyRight', '|',
    'bulletedList', 'numberedList', '|',
    'indent', '|',
    'insertLink', 'divider', 'blockquote', '|',
    'undo', 'redo',
  ],
}

const editorConfig: Partial<IEditorConfig> = {
  placeholder: '',
  MENU_CONF: {},
}

const form = ref({
  sender: '',
  recipients: '',
  subject: '',
  text: '',
  htmlContent: '',
  priority: 'NORMAL',
})

const rules: FormRules = {
  recipients: [{ required: true, message: () => t('validation.notBlank'), trigger: 'blur' }],
  subject: [{ required: true, message: () => t('validation.notBlank'), trigger: 'blur' }],
  text: [{
    validator: (_rule: unknown, _value: unknown, callback: (error?: Error) => void) => {
      const content = editorRef.value?.getText().trim() ?? ''
      if (!content) callback(new Error(t('validation.notBlank')))
      else callback()
    },
    trigger: 'custom',
  }],
}

function handleEditorCreated(editor: IDomEditor) {
  editorRef.value = editor
}

onMounted(async () => {
  try {
    const [groupsData, sendersData] = await Promise.all([
      listAllEmailGroups(),
      listSenders(),
    ])
    groups.value = groupsData
    senders.value = sendersData
    if (sendersData.length > 0) form.value.sender = sendersData[0]
  } catch {
    ElMessage.error(t('common.loadFailed'))
  }
})

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) editor.destroy()
})

function handleSenderSelect(sender: string) {
  form.value.sender = sender
}

async function handleGroupSelect(groupId: number | null) {
  if (!groupId) return
  try {
    const members = await listGroupMembers(groupId)
    const emails = members.filter((m: { enabled: number }) => m.enabled === 1).map((m: { email: string }) => m.email)
    const existing = form.value.recipients.split(/[;, ]+/).filter(Boolean)
    const merged = [...new Set([...existing, ...emails])]
    form.value.recipients = merged.join(', ')
  } catch {
    ElMessage.error(t('common.loadFailed'))
  }
}

function handleFileChange(file: UploadFile) {
  fileList.value.push(file)
}

function handleFileRemove(file: UploadFile) {
  fileList.value = fileList.value.filter(f => f.uid !== file.uid)
}

function handleExceed() {
  ElMessage.warning(t('compose.attachmentLimit'))
}

async function handleSend() {
  // Sync HTML content for validation
  form.value.text = editorRef.value?.getText().trim()
  // Trigger text field validation
  await formRef.value?.validateField('text').catch(() => {})
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  sending.value = true
  try {
    const formData = new FormData()
    formData.append('subject', form.value.subject)
    formData.append('text', form.value.htmlContent)
    formData.append('recipients', form.value.recipients)
    formData.append('priority', form.value.priority)
    if (form.value.sender) formData.append('sender', form.value.sender)
    for (const file of fileList.value) {
      if (file.raw) formData.append('attachment', file.raw)
    }
    await sendEmail(formData)
    ElMessage.success(t('compose.sendSuccess'))
    form.value = { sender: senders.value[0] ?? '', recipients: '', subject: '', text: '', htmlContent: '', priority: 'NORMAL' }
    editorRef.value?.clear()
    selectedGroup.value = null
    fileList.value = []
  } finally {
    sending.value = false
  }
}
</script>

<style scoped>
.recipients-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.recipients-input {
  flex: 1;
  min-width: 0;
}
.recipients-group-select {
  width: 180px;
  flex-shrink: 0;
}
.editor-form-item :deep(.el-form-item__content) {
  flex-direction: column;
  width: 100%;
}
.editor-wrapper {
  width: 100%;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
}
.editor-toolbar {
  border-bottom: 1px solid var(--el-border-color);
}
.editor-content {
  min-height: 300px;
  max-height: 400px;
  overflow-y: auto;
}
</style>
