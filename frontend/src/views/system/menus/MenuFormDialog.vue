<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="var(--rx-dialog-sm)" :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="var(--rx-form-label-width)">
      <el-form-item :label="t('menu.manage.type')">
        <el-radio-group v-model="form.menuType" :disabled="isEdit">
          <el-radio :value="MenuType.DIR">{{ t('menu.manage.typeDir') }}</el-radio>
          <el-radio :value="MenuType.MENU">{{ t('menu.manage.typeMenu') }}</el-radio>
          <el-radio :value="MenuType.BUTTON">{{ t('menu.manage.typeButton') }}</el-radio>
          <el-radio :value="MenuType.TAB">{{ t('menu.manage.typeTab') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('menu.manage.menuName')" prop="menuName">
        <el-input v-model="form.menuName" />
      </el-form-item>
      <el-form-item :label="t('menu.manage.title')" prop="title">
        <el-input v-model="form.title" :placeholder="t('menu.manage.titleHint')" />
      </el-form-item>
      <el-form-item v-if="form.menuType !== MenuType.BUTTON && form.menuType !== MenuType.TAB" :label="t('menu.manage.path')">
        <el-input v-model="form.path" placeholder="/monitor" />
      </el-form-item>
      <el-form-item v-if="form.menuType === MenuType.MENU" :label="t('menu.manage.component')">
        <el-input v-model="form.component" placeholder="views/Monitor.vue" />
      </el-form-item>
      <el-form-item :label="t('menu.manage.perms')">
        <el-select
          v-model="form.perms as string"
          filterable
          clearable
          :loading="permLoading"
          :placeholder="t('menu.manage.permsPlaceholder')"
          class="w-full"
        >
          <el-option
            v-for="p in permOptions"
            :key="p.permissionCode"
            :label="`${p.permissionCode}${p.permissionName && p.permissionName !== p.permissionCode ? ' - ' + p.permissionName : ''}`"
            :value="p.permissionCode as string"
          />
        </el-select>
        <div v-if="form.menuType === MenuType.TAB" class="hint mt4">{{ t('menu.manage.tabPermsHint') }}</div>
      </el-form-item>
      <el-form-item :label="t('menu.manage.icon')">
        <div class="icon-picker">
          <el-input v-model="form.icon" placeholder="Monitor / fa-solid fa-server">
            <template #prefix>
              <el-icon v-if="form.icon && !isFaIcon(form.icon) && hasIcon(form.icon)">
                <component :is="form.icon" />
              </el-icon>
              <FontAwesomeIcon
                v-else-if="form.icon && isFaIcon(form.icon)"
                :icon="faIconFor(form.icon)"
              />
              <el-icon v-else><Grid /></el-icon>
            </template>
          </el-input>
          <el-popover trigger="click" placement="bottom-start" :width="440" popper-class="icon-picker-popper">
            <template #reference>
              <el-button class="icon-picker-btn">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
            </template>
            <div class="icon-picker-panel">
              <el-tabs v-model="iconTab" size="small">
                <el-tab-pane :label="t('menu.manage.iconTabEp')" name="ep">
                  <div class="icon-picker-tabs">
                    <el-input
                      v-model="iconSearch"
                      :placeholder="t('menu.manage.iconSearch')"
                      clearable
                      size="small"
                      class="w-180"
                    />
                  </div>
                  <div class="icon-picker-list">
                    <div
                      v-for="name in filteredIcons"
                      :key="name"
                      :class="['icon-item', { active: form.icon === name }]"
                      @click="selectIcon(name)"
                    >
                      <el-icon :size="18"><component :is="name" /></el-icon>
                      <span class="icon-name">{{ name }}</span>
                    </div>
                    <div v-if="filteredIcons.length === 0" class="icon-empty">
                      {{ t('menu.manage.noMatchingIcon') }}
                    </div>
                  </div>
                </el-tab-pane>
                <el-tab-pane :label="t('menu.manage.iconTabFa')" name="fa">
                  <div class="icon-picker-tabs">
                    <el-input
                      v-model="faIconSearch"
                      :placeholder="t('menu.manage.iconSearch')"
                      clearable
                      size="small"
                      class="w-180"
                    />
                  </div>
                  <div class="icon-picker-list">
                    <div
                      v-for="item in filteredFaIcons"
                      :key="item.fullName"
                      :class="['icon-item', { active: form.icon === item.fullName }]"
                      :title="item.fullName"
                      @click="selectIcon(item.fullName)"
                    >
                      <FontAwesomeIcon :icon="item.icon" />
                      <span class="icon-name">{{ item.name }}</span>
                    </div>
                    <div v-if="filteredFaIcons.length === 0" class="icon-empty">
                      {{ t('menu.manage.noMatchingIcon') }}
                    </div>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </div>
          </el-popover>
        </div>
      </el-form-item>
      <el-form-item :label="t('common.sort')">
        <el-input-number v-model="form.sort" :min="0" :max="999" />
      </el-form-item>
      <el-form-item :label="t('common.status')">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitLoading" @click="onSubmit">{{ t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import * as Icons from '@element-plus/icons-vue'
import { FontAwesomeIcon, isFaIcon, faIconOr, FA_ICON_OPTIONS } from '@/icons'
import {
  createMenu,
  updateMenu,
  MenuType,
  type MenuTypeValue,
  type SysMenu,
} from '@/api/menu'
import { suggestPermissionCodes, type PermissionCode } from '@/api/permission'

const props = defineProps<{
  modelValue: boolean
  editData?: SysMenu | null
  parentId?: number | null
  menuTree: SysMenu[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'saved': []
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const isEdit = computed(() => !!props.editData?.id)
const dialogTitle = computed(() => isEdit.value ? t('common.edit') : t('menu.manage.addMenu'))

const formRef = ref()
const submitLoading = ref(false)
const permOptions = ref<PermissionCode[]>([])
const permLoading = ref(false)

const defaultForm = () => ({
  id: undefined as number | undefined,
  parentId: null as number | null,
  menuName: '',
  menuType: MenuType.MENU as MenuTypeValue,
  title: '',
  path: '',
  component: '',
  perms: '',
  icon: '',
  sort: 0,
  visible: 1,
  status: 1,
})
const form = reactive(defaultForm())

const formRules = {
  menuName: [{ required: true, message: () => t('menu.manage.nameRequired'), trigger: 'blur' }],
  title: [{ required: true, message: () => t('menu.manage.titleRequired'), trigger: 'blur' }],
}

// ==================== 图标选择器 ====================
const iconTab = ref<'ep' | 'fa'>('ep')
const iconSearch = ref('')
const faIconSearch = ref('')
const allIcons = Object.keys(Icons).filter((n) => !n.startsWith('_'))
const filteredIcons = computed(() => {
  const kw = iconSearch.value.trim().toLowerCase()
  if (!kw) return allIcons
  return allIcons.filter((n) => n.toLowerCase().includes(kw))
})
const filteredFaIcons = computed(() => {
  const kw = faIconSearch.value.trim().toLowerCase()
  if (!kw) return FA_ICON_OPTIONS
  return FA_ICON_OPTIONS.filter((i) => i.name.toLowerCase().includes(kw) || i.fullName.includes(kw))
})

const hasIcon = (name: string) => !!(Icons as Record<string, unknown>)[name]
const faIconFor = (value?: string) => faIconOr(value)

function selectIcon(name: string) {
  form.icon = name
}

// ==================== 权限码建议 ====================
function findMenuTitle(id?: number | null): string {
  if (!id) return ''
  const walk = (list: SysMenu[]): string => {
    for (const m of list || []) {
      if (m.id === id) return m.title || ''
      if (m.children?.length) {
        const r = walk(m.children)
        if (r) return r
      }
    }
    return ''
  }
  return walk(props.menuTree)
}

async function loadPermSuggest() {
  permLoading.value = true
  try {
    const menuTitle =
      form.menuType === MenuType.BUTTON || form.menuType === MenuType.TAB ? findMenuTitle(form.parentId) : form.title || ''
    const list = await suggestPermissionCodes({ menuTitle: menuTitle || undefined })
    if (form.perms && !list.some((p) => p.permissionCode === form.perms)) {
      list.unshift({ permissionCode: form.perms, permissionName: form.perms })
    }
    permOptions.value = list
  } catch {
    /* interceptor 已提示错误；权限码建议非关键功能 */
  } finally {
    permLoading.value = false
  }
}

watch(
  () => form.menuType,
  () => {
    if (visible.value) void loadPermSuggest()
  },
)

// ==================== 初始化表单 ====================
watch(() => [props.editData, props.parentId], () => {
  if (props.editData) {
    const row = props.editData
    Object.assign(form, defaultForm(), {
      id: row.id,
      parentId: row.parentId ?? null,
      menuName: row.menuName,
      menuType: row.menuType as MenuTypeValue,
      title: row.title,
      path: row.path || '',
      component: row.component || '',
      perms: row.perms || '',
      icon: row.icon || '',
      sort: row.sort ?? 0,
      visible: row.visible ?? 1,
      status: row.status ?? 1,
    })
  } else {
    Object.assign(form, defaultForm())
    form.parentId = props.parentId ?? null
    if (props.parentId) {
      const parent = props.menuTree.find((m) => m.id === props.parentId)
      if (parent) form.menuType = parent.menuType === MenuType.MENU ? MenuType.BUTTON : MenuType.MENU
    }
  }
  permOptions.value = []
  void loadPermSuggest()
}, { immediate: true })

// ==================== 提交 ====================
async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value && form.id) {
      await updateMenu(form.id, { ...form })
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createMenu({ ...form })
      ElMessage.success(t('common.createSuccess'))
    }
    emit('saved')
    visible.value = false
  } finally {
    submitLoading.value = false
  }
}
</script>