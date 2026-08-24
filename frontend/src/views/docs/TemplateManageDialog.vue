<template>
  <el-dialog v-model="visible" :title="$t('docs.templateManage')" width="760px">
    <div class="toolbar">
      <el-input v-model="form.name" :placeholder="$t('docs.tplName')" size="small" class="w-160" />
      <el-input v-model="form.category" :placeholder="$t('docs.tplCategory')" size="small" class="w-120" />
      <el-select v-model="form.docType" size="small" class="w-120">
        <el-option v-for="tp in textTypes" :key="tp" :label="$t(docTypeKey(tp))" :value="tp" />
      </el-select>
      <el-button size="small" type="primary" @click="() => openCreate()">{{ $t('docs.addTemplate') }}</el-button>
    </div>
    <el-table :data="templates" size="small" border>
      <el-table-column prop="name" :label="$t('docs.tplName')" min-width="130" />
      <el-table-column prop="category" :label="$t('docs.tplCategory')" width="100" />
      <el-table-column :label="$t('docs.tplType')" width="100">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ $t(docTypeKey(row.docType)) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdBy" :label="$t('docs.author')" width="100" />
      <el-table-column :label="$t('common.operation')" width="130">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openEdit(row as TemplateItem)">{{ $t('common.edit') }}</el-button>
          <el-button size="small" link type="danger" @click="remove(row as TemplateItem)">{{ $t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑模板（含内容编辑器） -->
    <el-dialog v-model="editVisible" :title="editing ? $t('docs.editTemplate') : $t('docs.addTemplate')" width="680px" append-to-body>
      <el-form label-width="90px">
        <el-form-item :label="$t('docs.tplName')" required>
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item :label="$t('docs.tplCategory')">
          <el-input v-model="editForm.category" />
        </el-form-item>
        <el-form-item :label="$t('docs.tplType')">
          <el-select v-model="editForm.docType" class="w-full">
            <el-option v-for="tp in textTypes" :key="tp" :label="$t(docTypeKey(tp))" :value="tp" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('docs.tplContent')">
          <MarkdownEditor v-if="editForm.docType === 'MARKDOWN'" v-model="editForm.content" />
          <el-input v-else v-model="editForm.content" type="textarea" :rows="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createTemplate, deleteTemplate, listTemplates, updateTemplate,
  type DocType, type TemplateItem,
} from '@/api/doc'
const MarkdownEditor = defineAsyncComponent(() => import('./MarkdownEditor.vue'))

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  /** 模板列表变化（新增/删除/编辑），父组件刷新编辑弹窗的模板下拉 */
  (e: 'changed'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const { t } = useI18n()
const templates = ref<TemplateItem[]>([])
/** 模板可用类型：正文型（二进制模板无意义） */
const textTypes = ['MARKDOWN', 'TEXT', 'HTML'] as const

const docTypeKey = (docType?: DocType) =>
  `docs.docType${(docType || 'MARKDOWN').charAt(0) + (docType || 'MARKDOWN').slice(1).toLowerCase()}`

const form = reactive({ name: '', category: '', docType: 'MARKDOWN' as DocType })
const editVisible = ref(false)
const editing = ref<TemplateItem | null>(null)
const editForm = reactive({ name: '', category: '', content: '', docType: 'MARKDOWN' as DocType })

const load = async () => {
  try {
    templates.value = await listTemplates()
  } catch {
    templates.value = []
  }
}

watch(visible, (open) => {
  if (open) void load()
})

const openCreate = () => {
  editing.value = null
  editForm.name = form.name.trim()
  editForm.category = form.category.trim() || t('docs.defaultCategory')
  editForm.docType = form.docType
  editForm.content = ''
  editVisible.value = true
}

const openEdit = (row: TemplateItem) => {
  editing.value = row
  editForm.name = row.name
  editForm.category = row.category
  editForm.docType = row.docType || 'MARKDOWN'
  editForm.content = row.content || ''
  editVisible.value = true
}

const save = async () => {
  if (!editForm.name.trim()) {
    ElMessage.warning(t('docs.tplNameRequired'))
    return
  }
  const payload = {
    name: editForm.name.trim(),
    category: editForm.category.trim() || t('docs.defaultCategory'),
    content: editForm.content,
    docType: editForm.docType,
  }
  if (editing.value) {
    await updateTemplate(editing.value.id, payload)
  } else {
    await createTemplate(payload)
  }
  editVisible.value = false
  form.name = ''
  form.category = ''
  ElMessage.success(t('common.save'))
  await load()
  emit('changed')
}

const remove = async (row: TemplateItem) => {
  await ElMessageBox.confirm(t('docs.deleteTplConfirm'), t('common.confirm'), { type: 'warning' })
  await deleteTemplate(row.id)
  await load()
  emit('changed')
}
</script>