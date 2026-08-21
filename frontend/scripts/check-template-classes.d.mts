/**
 * check-template-classes.mjs 的类型声明（供 vue-tsc / vitest 导入时类型检查）。
 * 实现见同目录 check-template-classes.mjs。
 */

export interface UndefinedClassHit {
  /** 相对 frontend 根的路径（如 src/views/xxx.vue） */
  file: string
  /** 1 起始行号 */
  line: number
  /** 未定义的 class 名 */
  className: string
}

export interface StyleScopes {
  /** <style scoped> 块：仅本组件模板 + 子组件根元素可用 */
  scoped: Set<string>
  /** 未标 scoped 的 <style> 块：任何组件可用 */
  global: Set<string>
  /** scoped 块内 :deep(.x) 穿透选择器：对任意后代生效 */
  deep: Set<string>
}

/** 递归收集指定扩展名的文件 */
export function walk(dir: string, exts: string[]): string[]

/** 提取一段 .vue 源码 <template> 段内的 class（静态 + :class 绑定字面量，:style 已屏蔽），返回 class → 行号列表 */
export function extractTemplateClasses(content: string): Map<string, number[]>

/** 从全局 CSS 内容提取已定义 class 集合 */
export function extractDefinedClasses(css: string): Set<string>

/**
 * 提取「顶层」类选择器（SCSS 感知）：选择器嵌套/@mixin 内类不算定义，
 * @media/@supports 等 at-rule 条件包装内类算定义。
 */
export function extractTopLevelClasses(css: string): Set<string>

/** 提取 .vue 文件内 <style> 块的类定义，按 scoped / 全局 / :deep 穿透 分桶 */
export function extractVueStyleScopes(content: string): StyleScopes

/** 兼容旧导出：全部 style 块并集 */
export function extractVueDefinedClasses(content: string): Set<string>

/** 提取 import 的 .vue 组件：Map<组件名, 源文件绝对路径> */
export function extractImportedVueComponents(
  content: string,
  fileDir: string,
  srcDir: string,
): Map<string, string>

/** 提取模板中使用的组件标签名（PascalCase，排除 el-/Fa 与原生元素） */
export function extractTemplateTags(content: string): Set<string>

/** 提取模板根元素的 class 列表（父组件 scoped 样式对子组件根元素生效） */
export function extractRootClasses(content: string): string[]

/** 检测 :class 对象中的动态计算键（{ [dynamicClass]: true }）—— 变量 key 无法静态校验，供 CLI 提示（不失败） */
export function detectDynamicClassKeys(content: string): { line: number; expr: string }[]

/** 白名单判定（el- 前缀、fa- 前缀、v-enter 等第三方或动态注入 class） */
export function isAllowedClass(name: string): boolean

/**
 * 扫描模板自定义 class 是否在「适用作用域」中有定义：
 * 全局（common.css + 未标 scoped 的 style 块 + :deep 穿透）／ 本组件 style ／ 父组件 scoped 的子根豁免。
 * @returns 未定义 class 的命中列表（空数组即通过）
 */
export function findUndefinedTemplateClasses(
  vueFiles: string[],
  globalCssFiles: string[],
  srcDir?: string,
): UndefinedClassHit[]
