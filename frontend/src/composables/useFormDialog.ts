/**
 * 通用 CRUD 表单弹窗 Composable
 *
 * 统一项目中 10+ 个视图的对话框模式：
 *   dialogVisible / isEdit / loading / formRef / form / rules / openCreate / openEdit / onSubmit
 *
 * 约定：
 * - form 使用 ref<T> 包装（模板中 Vue 自动解包，仍可直接 v-model="form.xxx"）
 * - 创建/更新通过 createApi / updateApi 注入，或统一用 saveApi
 * - 表单验证通过 el-form 的 formRef.validate() 执行
 * - 保存成功后自动关闭弹窗并调用 onSuccess 回调
 *
 * @example 标准用法（创建 + 更新分离）
 * const { dialogVisible, dialogTitle, isEdit, loading, formRef, form, rules, openCreate, openEdit, onSubmit } =
 *   useFormDialog<NoticeForm>({
 *     defaultForm: () => ({ id: undefined, title: '', content: '', status: 1 }),
 *     rules: { title: [{ required: true, message: '必填', trigger: 'blur' }] },
 *     createApi: (data) => createNotice(data),
 *     updateApi: (id, data) => updateNotice(id, data),
 *     onSuccess: () => refresh(),
 *     i18nPrefix: 'notice',
 *   })
 *
 * @example 单一 saveApi（脚本/调度等）
 * const { dialogVisible, dialogTitle, isEdit, loading, form, openCreate, openEdit, onSubmit } =
 *   useFormDialog<ScriptForm>({
 *     defaultForm: () => ({ name: '', command: '', tags: '', favorite: false }),
 *     saveApi: async (isEdit, data) => {
 *       if (isEdit && data.id) await updateScript(data.id, data)
 *       else await createScript(data)
 *     },
 *     onSuccess: () => fetchData({}, true),
 *   })
 */
import { computed, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

export interface UseFormDialogOptions<T extends object> {
  /** 默认表单对象工厂（创建时重置用） */
  defaultForm: () => T
  /** el-form 验证规则 */
  rules?: FormRules
  /** 单一保存 API：接收 (isEdit, data)，内部判断创建/更新 */
  saveApi?: (isEdit: boolean, data: T) => Promise<unknown>
  /** 创建 API */
  createApi?: (data: T) => Promise<unknown>
  /** 更新 API：接收 (id, data) */
  updateApi?: (id: number | string, data: T) => Promise<unknown>
  /** 保存成功后回调（通常用于刷新列表） */
  onSuccess?: () => void
  /** 国际化前缀，用于自动拼接"新增"标题（如 'notice' → t('notice.add')） */
  i18nPrefix?: string
  /** 是否在保存前执行表单验证，默认 true */
  validate?: boolean
}

export interface UseFormDialogReturn<T extends object> {
  dialogVisible: Ref<boolean>
  dialogTitle: Ref<string>
  isEdit: Ref<boolean>
  loading: Ref<boolean>
  formRef: Ref<FormInstance | undefined>
  form: Ref<T>
  rules: FormRules | undefined
  openCreate: (extra?: Partial<T>) => void
  openEdit: (row: Partial<T> & { id?: number | string }) => void
  onSubmit: () => Promise<void>
}

export function useFormDialog<T extends object>(
  options: UseFormDialogOptions<T>,
): UseFormDialogReturn<T> {
  const { t } = useI18n()

  const dialogVisible = ref(false)
  const isEdit = ref(false)
  const loading = ref(false)
  const formRef = ref<FormInstance>()
  const form = ref(options.defaultForm()) as Ref<T>

  const dialogTitle = computed(() => {
    if (isEdit.value) return t('common.edit')
    if (options.i18nPrefix) {
      // 兜底：命名空间缺 add 键时（intlify 返回原 key），回退 common.create，避免弹窗标题显示原始 key
      const label = t(`${options.i18nPrefix}.add`)
      return label === `${options.i18nPrefix}.add` ? t('common.create') : label
    }
    return t('common.create')
  })

  const openCreate = (extra?: Partial<T>) => {
    isEdit.value = false
    form.value = { ...options.defaultForm(), ...extra } as T
    dialogVisible.value = true
  }

  const openEdit = (row: Partial<T> & { id?: number | string }) => {
    isEdit.value = true
    form.value = { ...options.defaultForm(), ...row } as T
    dialogVisible.value = true
  }

  const onSubmit = async () => {
    if (options.validate !== false && formRef.value) {
      try {
        await formRef.value.validate()
      } catch {
        return
      }
    }
    loading.value = true
    try {
      if (options.saveApi) {
        await options.saveApi(isEdit.value, { ...form.value })
      } else if (isEdit.value && options.updateApi) {
        const id = (form.value as { id?: number | string }).id
        if (id == null) return
        await options.updateApi(id, { ...form.value })
      } else if (options.createApi) {
        await options.createApi({ ...form.value })
      }
      ElMessage.success(isEdit.value ? t('common.updateSuccess') : t('common.addSuccess'))
      dialogVisible.value = false
      options.onSuccess?.()
    } finally {
      loading.value = false
    }
  }

  return {
    dialogVisible,
    dialogTitle,
    isEdit,
    loading,
    formRef,
    form,
    rules: options.rules,
    openCreate,
    openEdit,
    onSubmit,
  }
}