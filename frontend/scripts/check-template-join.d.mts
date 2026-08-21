/**
 * check-template-join.mjs 的类型声明（供 vue-tsc / vitest 导入时类型检查）。
 * 实现见同目录 check-template-join.mjs。
 */

export interface JoinedSlotHit {
  /** 1 起始行号 */
  line: number
  /** 该行去首尾空白后的前 120 字符 */
  text: string
}

/** 递归收集 .vue 文件 */
export function walk(dir: string): string[]

/** 检测一段 .vue 源码中的「开标签与 <template #default> 拼行」回归 */
export function findJoinedTemplateSlots(content: string): JoinedSlotHit[]
