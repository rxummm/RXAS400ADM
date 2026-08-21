#!/usr/bin/env node
/**
 * check-template-classes.mjs —— 模板自定义 class 样式定义一致性扫描（2026-08-16 新增，同日增强 v2）：
 *
 * 背景：MenuFormDialog.vue 的图标选择器用了 icon-picker-list / icon-item 等 class，
 * 但组件既无 <style> 块、common.css 也无对应规则，导致图标竖排（样式丢失）。
 * 这类问题编译/门禁全部拦截不到，需要模板 class ↔ 样式定义交叉核对。
 *
 * 规则（v2 增强：scoped 作用域 + 组件树 + :style 屏蔽）：
 * 1) 提取所有 .vue 模板里的自定义 class（静态 class="a b"、:class 数组/对象字面量中的字符串键）
 * 2) 样式定义按作用域分桶：
 *    - 全局：common.css / theme.css + 所有「未标 scoped」的 <style> 块（任何组件可用）
 *    - scoped：<style scoped> 块 —— 仅本组件模板可用；父组件 scoped 仅对「子组件根元素」生效
 *    - :deep() 内的类选择器穿透组件边界，视为全局
 * 3) 组件树：解析每个 .vue 的 import 与模板标签，建立 父组件 → 子组件 映射，
 *    子组件根元素上的 class 允许引用父组件的 scoped 定义（Vue scoped 真实行为）
 * 4) :style="..." 绑定为内联样式，屏蔽后不提取其中的字符串（防误报）
 * 5) 白名单：el-*（Element Plus）、fa-*（Font Awesome）
 * 6) 模板用到的 class 在任何作用域都找不到 → 报错（样式丢失，如 icon-picker）
 *
 * 用法：node scripts/check-template-classes.mjs   （已并入 npm run lint / verify-all / frontend CI）
 * 测试：findUndefinedTemplateClasses / extractTemplateClasses 由 vitest 导入（src/__tests__/gates.test.ts）
 */
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, extname, dirname, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

/** 递归收集指定后缀文件 */
export function walk(dir, exts) {
  const out = []
  for (const name of readdirSync(dir)) {
    const full = join(dir, name)
    if (statSync(full).isDirectory()) {
      out.push(...walk(full, exts))
    } else if (exts.includes(extname(full).toLowerCase())) {
      out.push(full)
    }
  }
  return out
}

// ---- 模板 class 提取 ----

// 静态 class="a b c"（负向断言排除 :class= 与 popper-class= 前缀）
const STATIC_CLASS = /(?<![:\\w-])class="([^"]+)"/g
// :class 表达式内容（数组/对象字面量）：:class="['a', { 'b': c, d: e }]"
const BIND_CLASS = /:class="([^"]+)"/g
// 表达式里的字符串字面量
const STR_LITERAL = /'([^']+)'/g

/**
 * 屏蔽 :style="..." 绑定内容（内联样式，其中的字符串不是 class）。
 * 替换为等长占位符保持行号不变。
 */
function maskInlineStyles(templateText) {
  const MASK_STYLE = /:style="[^"]*"/g
  return templateText.replace(MASK_STYLE, (m) => ' '.repeat(m.length))
}

/**
 * 提取一段 .vue 源码模板中用到的自定义 class（排除动态表达式与 :style 内联样式）。
 * @param {string} content .vue 文件内容
 * @returns {Map<string, number[]>} class → 行号列表
 */
export function extractTemplateClasses(content) {
  const found = new Map()
  const add = (name, line) => {
    if (!name || /^[0-9]/.test(name)) return
    if (!found.has(name)) found.set(name, [])
    if (!found.get(name).includes(line)) found.get(name).push(line)
  }
  // 只扫描 <template> 段（排除 <script>/<style> 里的字符串干扰）
  let templateStart = 0
  let templateEnd = content.length
  const tStart = content.indexOf('<template>')
  const tEnd = content.lastIndexOf('</template>')
  if (tStart !== -1 && tEnd !== -1) {
    templateStart = tStart
    templateEnd = tEnd
  }
  let templateText = content.slice(templateStart, templateEnd)
  // :style 内联样式屏蔽（其中的字符串不是 class 引用）
  templateText = maskInlineStyles(templateText)

  // 静态 class
  let m
  while ((m = STATIC_CLASS.exec(templateText)) !== null) {
    const line = countLines(content, m.index)
    for (const name of m[1].split(/\s+/).filter(Boolean)) add(name, line)
  }
  // :class 绑定
  while ((m = BIND_CLASS.exec(templateText)) !== null) {
    const expr = m[1]
    const line = countLines(content, m.index)
    // 字符串字面量（数组项 / 引号键 / 三元分支 / 动态计算键内的字面量）
    let s
    while ((s = STR_LITERAL.exec(expr)) !== null) {
      const before = expr.slice(0, s.index).trimEnd().slice(-1)
      // 排除函数实参（如 startsWith('FAILED')）与动态模板串（${} / 拼接）
      if (before === '(') continue
      if (s[1].includes('${') || s[1].includes('+')) continue
      add(s[1], line)
    }
    // 对象裸键（如 { active: x }）——只取对象字面量部分，排除三元 ? : 误抓
    const objectPart = expr.slice(expr.indexOf('{'), expr.lastIndexOf('}') + 1)
    if (objectPart.includes('{')) {
      const keyRe = /([A-Za-z_][\w-]*)\s*:/g
      let k
      while ((k = keyRe.exec(objectPart)) !== null) {
        // 排除动态计算键 [dynamicClass]: —— 变量 key 运行时才定，无法静态校验（不误抓，也不当作 class 定义）
        const before = objectPart.slice(0, k.index)
        const lastOpen = before.lastIndexOf('[')
        const lastClose = before.lastIndexOf(']')
        if (lastOpen > lastClose) continue
        add(k[1], line)
      }
    }
  }
  // popper-class（teleport 到 body，必须全局定义）
  const POPPER = /popper-class="([^"]+)"/g
  while ((m = POPPER.exec(templateText)) !== null) {
    add(m[1], countLines(content, m.index))
  }

  return found
}

function countLines(content, index) {
  return content.slice(0, index).split('\n').length
}

/**
 * 检测 :class 对象中的动态计算键（{ [dynamicClass]: true }）—— 变量 key 运行时才定值，
 * 静态扫描无法校验其样式定义，返回行号与表达式供 CLI 提示（不失败，避免误报）。
 * @param {string} content .vue 文件内容
 * @returns {{ line: number; expr: string }[]}
 */
export function detectDynamicClassKeys(content) {
  const hits = []
  let templateStart = 0
  let templateEnd = content.length
  const tStart = content.indexOf('<template>')
  const tEnd = content.lastIndexOf('</template>')
  if (tStart !== -1 && tEnd !== -1) {
    templateStart = tStart
    templateEnd = tEnd
  }
  const templateText = maskInlineStyles(content.slice(templateStart, templateEnd))
  let m
  while ((m = BIND_CLASS.exec(templateText)) !== null) {
    const expr = m[1]
    // 计算键：[ 开头、含 ]: 的键（如 [dynamicClass]:、[flag ? 'a' : 'b']:）
    const keyRe = /\[([^[\]]*)\]\s*:/g
    let k
    while ((k = keyRe.exec(expr)) !== null) {
      const inner = k[1].trim()
      if (!inner) continue
      hits.push({ line: countLines(content, m.index), expr: inner })
    }
  }
  return hits
}

// ---- 样式定义提取（v2：按 scoped 作用域分桶） ----

/** 提取 CSS 文本中的类选择器（.foo / .foo.bar / html.dark .foo 等） */
export function extractDefinedClasses(css) {
  const set = new Set()
  const re = /\.([_a-zA-Z][_a-zA-Z0-9-]*)/g
  let m
  while ((m = re.exec(css)) !== null) {
    set.add(m[1])
  }
  return set
}

/**
 * at-rule 条件包装白名单：这些块内类编译后仍为顶层类（模板 class 会命中），
 * 与之相对的是选择器嵌套（SCSS 的 .a { .b {} } → .a .b，模板 class="b" 不生效）。
 */
const AT_RULE_WRAP = ['@media', '@supports', '@container', '@layer', '@document', '@keyframes']

/**
 * 提取「顶层」类选择器（v2.1，SCSS 感知）：
 * - 选择器块内嵌套的类（.a { .b {} }）不算定义（编译后 .a .b，单独 class="b" 不生效）
 * - @media/@supports 等 at-rule 条件包装内的类算定义（编译后平铺为顶层类）
 * - @mixin/@include/@if 等 SCSS 控制流块内的类不算定义（未 include/分支未编译时不产出）
 * @param {string} css CSS/SCSS 文本
 * @returns {Set<string>}
 */
export function extractTopLevelClasses(css) {
  const set = new Set()
  const stack = [] // 每层：true = at-rule 条件包装（穿透），false = 选择器块（嵌套）
  let stmtStart = 0
  let i = 0
  while (i < css.length) {
    const ch = css[i]
    if (ch === '{') {
      const head = css.slice(stmtStart, i).trim()
      const isAtWrap = AT_RULE_WRAP.some((at) => head === at || head.startsWith(at + ' ') || head.startsWith(at + '('))
      stack.push(isAtWrap)
      i++
      stmtStart = i
      continue
    }
    if (ch === '}') {
      stack.pop()
      i++
      stmtStart = i
      continue
    }
    if (ch === ';') {
      stmtStart = i + 1
      i++
      continue
    }
    if (ch === '.') {
      // 锚定当前位置：/^/ 防止 exec 跳过 .5px 等数字点号误匹配后面的类名（如 .b），
      // 否则 i 会跳到错误位置导致 `}` 被吞、栈不 pop
      const m = /^\.([_a-zA-Z][_a-zA-Z0-9-]*)/.exec(css.slice(i))
      if (m) {
        // 从栈底数连续穿透层数；若所有打开层都是 at-rule 包装（无选择器块）→ 顶层类
        let atDepth = 0
        for (let k = stack.length - 1; k >= 0; k--) {
          if (stack[k]) atDepth++
          else break
        }
        if (stack.length === atDepth) set.add(m[1])
        i += m[0].length
        continue
      }
    }
    i++
  }
  return set
}

/**
 * 提取 .vue 文件内 <style> 块中的类定义，按作用域分桶。
 * @param {string} content .vue 文件内容
 * @returns {{ scoped: Set<string>, global: Set<string>, deep: Set<string> }}
 *   scoped — <style scoped> 块（仅本组件 + 子组件根元素可用）
 *   global — 未标 scoped 的 <style> 块（任何组件可用）
 *   deep   — scoped 块内 :deep(.x) 穿透选择器（对任意后代生效，视为全局）
 */
export function extractVueStyleScopes(content) {
  const scoped = new Set()
  const global = new Set()
  const deep = new Set()
  const styleRe = /<style([^>]*)>([\s\S]*?)<\/style>/g
  let m
  while ((m = styleRe.exec(content)) !== null) {
    const attrs = m[1]
    const css = m[2]
    // 穿透选择器内的类 → 全局可用：:deep(.x) / ::v-deep(.x) / ::v-global(.x)（scoped 穿透），
    // :slotted(.x) / ::v-slotted(.x)（插槽内容穿透：插槽由父组件渲染，子 scoped 需显式命中）
    const deepRe = /(?::deep|::v-deep|::v-global|::v-slotted|:slotted)\(([^)]*)\)/g
    let d
    while ((d = deepRe.exec(css)) !== null) {
      for (const c of extractDefinedClasses(d[1])) deep.add(c)
    }
    const isScoped = /\bscoped\b/.test(attrs)
    // 顶层类提取（SCSS 感知：选择器嵌套/@mixin 内类不算定义，@media 等条件包装内算）
    for (const c of extractTopLevelClasses(css)) {
      if (deep.has(c)) continue
      if (isScoped) scoped.add(c)
      else global.add(c)
    }
  }
  return { scoped, global, deep }
}

/** 兼容旧导出（v1 曾用）：全部 style 块并集（新调用方请用 extractVueStyleScopes） */
export function extractVueDefinedClasses(content) {
  const { scoped, global, deep } = extractVueStyleScopes(content)
  return new Set([...scoped, ...global, ...deep])
}

// ---- 组件树（v2：父 scoped 仅对子组件根元素生效） ----

/** 提取 import 的 .vue 组件：Map<组件名, 源文件路径（已解析）> */
export function extractImportedVueComponents(content, fileDir, srcDir) {
  const map = new Map()
  const IMPORT_RE = /import\s+([A-Za-z_$][\w$]*)\s+from\s+['"]([^'"]+\.vue)['"]/g
  let m
  while ((m = IMPORT_RE.exec(content)) !== null) {
    const name = m[1]
    let spec = m[2]
    if (spec.startsWith('@/')) spec = join(srcDir, spec.slice(2))
    else if (spec.startsWith('.')) spec = resolve(fileDir, spec)
    else continue // 第三方包，忽略
    map.set(name, spec)
  }
  return map
}

/** PascalCase → kebab-case（As400ServerSelector → as400-server-selector） */
function toKebab(name) {
  return name
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/([A-Z])([A-Z][a-z])/g, '$1-$2')
    .toLowerCase()
}

/** 提取模板中使用的组件标签名（PascalCase，排除 el- 前缀、Fa 图标与原生元素） */
export function extractTemplateTags(content) {
  const tags = new Set()
  const tStart = content.indexOf('<template>')
  const tEnd = content.lastIndexOf('</template>')
  if (tStart === -1 || tEnd === -1) return tags
  const templateText = content.slice(tStart, tEnd)
  // PascalCase：<As400ServerSelector
  const PASCAL = /<([A-Z][A-Za-z0-9]*)(?=[\s/>])/g
  let m
  while ((m = PASCAL.exec(templateText)) !== null) {
    if (!m[1].startsWith('el-') && !m[1].startsWith('Fa')) tags.add(m[1])
  }
  return tags
}

/**
 * 提取模板根元素的 class 列表（父组件 scoped 样式对子组件根元素生效）。
 * 根元素 = <template> 后第一个顶层元素（跳过注释/空白）。
 */
export function extractRootClasses(content) {
  const tStart = content.indexOf('<template>')
  if (tStart === -1) return []
  const rest = content.slice(tStart + '<template>'.length)
  // 跳到第一个元素标签（跳过注释 <!-- --> 与空白）
  const tagMatch = /^\s*(?:<!--[\s\S]*?-->\s*)*<([A-Za-z][\w.-]*)/.exec(rest)
  if (!tagMatch) return []
  const tagStart = rest.indexOf('<', rest.indexOf(tagMatch[1]) - 1)
  // 从标签起始解析到开始标签结束（不跨 > 引号），提取 class
  let i = tagStart
  let quote = null
  let end = -1
  for (; i < rest.length; i++) {
    const ch = rest[i]
    if (quote) {
      if (ch === quote) quote = null
    } else if (ch === '"' || ch === "'") {
      quote = ch
    } else if (ch === '>') {
      end = i
      break
    }
  }
  if (end === -1) return []
  const openTag = rest.slice(tagStart, end + 1)
  const cls = /(?<![:\\w-])class="([^"]+)"/.exec(openTag)
  if (!cls) return []
  return cls[1].split(/\s+/).filter(Boolean)
}

// ---- 白名单 ----

/** Element Plus / Font Awesome 等第三方类名前缀 */
const ALLOWED_PREFIXES = ['el-', 'fa-', 'fa-solid', 'fa-regular', 'fa-brands', 'v-enter', 'v-leave', 'fade-transform']

/** 白名单全量判定 */
export function isAllowedClass(name) {
  return ALLOWED_PREFIXES.some((p) => name.startsWith(p))
}

/**
 * 扫描：模板自定义 class 是否在「适用作用域」中有定义。
 * 判定（对文件 F 中使用的类 C）：
 *   1. 白名单 → 通过
 *   2. C ∈ 全局集合（common.css/theme.css + 任意文件未标 scoped 的 <style> + 任意 :deep()） → 通过
 *   3. C ∈ F 自身的 <style> 块（scoped 或非） → 通过
 *   4. C ∈ F 的父组件 scoped 集合，且 C 用在 F 的根元素上 → 通过（父 scoped 作用于子根）
 *   否则 → 报错（样式丢失）
 * @param {string[]} vueFiles .vue 文件绝对路径列表
 * @param {string[]} globalCssFiles 全局 CSS（common.css 等）绝对路径列表
 * @param {string} [srcDir] src 目录（解析 @/ 别名，默认取 vueFiles 首个文件的 src 祖目录）
 * @returns {{ file: string; line: number; className: string }[]}
 */
export function findUndefinedTemplateClasses(vueFiles, globalCssFiles, srcDir) {
  // 全局集合：所有「未标 scoped」style 块 + 所有 :deep() 类 + 全局 CSS
  const globalSet = new Set()
  const scopedMap = new Map() // 文件 → 自身 scoped 集合
  const globalOwn = new Map() // 文件 → 自身未标 scoped 集合（用于提示，判定走 globalSet）
  const fileContent = new Map()
  const parents = new Map() // 文件 → 父组件文件列表

  if (!srcDir) {
    srcDir = vueFiles[0] ? dirname(vueFiles[0]).split(/[\\/]src[\\/]/)[0] + '/src' : null
  }

  // 第一遍：解析每个文件的样式作用域 + import 组件 + 模板标签
  const imports = new Map() // 文件 → Map<组件名, 绝对路径>
  const templateTags = new Map() // 文件 → Set<标签名>
  for (const f of vueFiles) {
    const content = readFileSync(f, 'utf-8')
    fileContent.set(f, content)
    const { scoped, global, deep } = extractVueStyleScopes(content)
    scopedMap.set(f, scoped)
    globalOwn.set(f, global)
    for (const c of global) globalSet.add(c)
    for (const c of deep) globalSet.add(c)
    imports.set(f, extractImportedVueComponents(content, dirname(f), srcDir))
    templateTags.set(f, extractTemplateTags(content))
  }
  for (const f of globalCssFiles) {
    for (const c of extractDefinedClasses(readFileSync(f, 'utf-8'))) globalSet.add(c)
  }

  // 第二遍：建立父子关系 —— 文件 A 的模板中使用了 import 的组件 C（标签名=组件名）
  // → C 所在文件的父组件是 A；A 的 scoped 样式仅对 C 的根元素生效
  for (const [parentFile, tags] of templateTags) {
    const imp = imports.get(parentFile) || new Map()
    for (const [name, absPath] of imp) {
      // 模板中确实使用了该组件标签（PascalCase 同名，或 kebab-case 变体）
      const used = [...tags].some(
        (tag) =>
          tag === name ||
          tag.toLowerCase() === name.toLowerCase() ||
          tag.toLowerCase() === toKebab(name),
      )
      if (!used) continue
      if (!parents.has(absPath)) parents.set(absPath, [])
      if (!parents.get(absPath).includes(parentFile)) parents.get(absPath).push(parentFile)
    }
  }

  // 第三遍：判定每个文件的使用
  const bad = []
  for (const f of vueFiles) {
    const rel = f.replaceAll('\\', '/').replace(process.cwd().replaceAll('\\', '/'), '.')
    const classes = extractTemplateClasses(fileContent.get(f))
    const myScoped = scopedMap.get(f)
    const myGlobal = globalOwn.get(f)
    const rootClasses = extractRootClasses(fileContent.get(f))
    // 父组件 scoped 集合
    const parentScoped = new Set()
    for (const p of parents.get(f) || []) {
      for (const c of scopedMap.get(p) || []) parentScoped.add(c)
    }
    for (const [name, lines] of classes) {
      if (isAllowedClass(name)) continue
      if (globalSet.has(name)) continue
      if (myScoped.has(name) || myGlobal.has(name)) continue
      // 父 scoped 仅对子组件根元素生效
      if (parentScoped.has(name) && rootClasses.includes(name)) continue
      for (const line of lines) bad.push({ file: rel, line, className: name })
    }
  }
  return bad
}

// ---- CLI 入口（被 vitest 导入时不执行） ----
const isMain = process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href
if (isMain) {
  const SRC = join(fileURLToPath(new URL('../src', import.meta.url)))
  const vueFiles = walk(SRC, ['.vue'])
  const globalCss = [join(SRC, 'styles/common.css'), join(SRC, 'styles/theme.css')].filter((f) => {
    try {
      statSync(f)
      return true
    } catch {
      return false
    }
  })

  const bad = findUndefinedTemplateClasses(vueFiles, globalCss, SRC)

  if (bad.length) {
    console.error(`❌ ${bad.length} 处模板自定义 class 无适用作用域样式定义（组件自身/全局/父组件根元素均无）:`)
    bad.forEach((b) => console.error(`   - ${b.file}:${b.line}: .${b.className}`))
    console.error('   若该 class 由第三方组件/动态注入提供，请加入 ALLOWED_PREFIXES；否则补齐 scoped 或 common.css 样式。')
    process.exit(1)
  }

  // 动态计算键提示（不失败：变量 key 运行时才定值，无法静态校验，但字面量分支已被提取校验）
  let dynCount = 0
  for (const f of vueFiles) {
    const hits = detectDynamicClassKeys(readFileSync(f, 'utf-8'))
    for (const h of hits) {
      dynCount++
      console.warn(`   ⚠ ${f.replaceAll('\\', '/').replace(SRC.replaceAll('\\', '/'), 'src')}:${h.line} :class 动态计算键 [${h.expr}] 无法静态校验，请确认运行时 class 有样式定义`)
    }
  }
  if (dynCount) {
    console.warn(`⚠ ${dynCount} 处 :class 动态计算键（变量 key）无法静态校验（仅提示，不阻断；字面量分支仍会校验）`)
  }

  console.log(`✅ 模板自定义 class 全部有适用作用域样式定义（扫描 ${vueFiles.length} 个 .vue + ${globalCss.length} 个全局 CSS，含 scoped 作用域与组件树判定）`)
}