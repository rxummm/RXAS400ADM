/**
 * check-i18n.mjs 的类型声明（供 vue-tsc / vitest 导入时类型检查）。
 * 实现见同目录 check-i18n.mjs。
 */

export interface I18nPrefixMissingAddHit {
  /** 命名空间，如 'permissions'（缺 ${ns}.add） */
  ns: string
  /** 绝对路径 */
  file: string
  /** 1 起始行号 */
  line: number
}

/** 提取所有文件中的 $t('a.b.c') 静态引用 key 集合（已剥离注释/动态拼接） */
export function extractRefs(files: string[]): Set<string>

/**
 * M7：useFormDialog 的 i18nPrefix 命名空间必须含 add 键（新增弹窗标题用 ${prefix}.add）。
 * @returns 缺 add 键的命名空间命中列表（空数组即通过）
 */
export function findI18nPrefixMissingAdd(
  files: string[],
  zhKeys: Set<string>,
  enKeys: Set<string>,
): I18nPrefixMissingAddHit[]
