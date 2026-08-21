#!/usr/bin/env node
/**
 * i18n key 校验（M4）：
 * 1) 扫描 src 下所有 .vue/.ts 中的 $t('a.b.c') 静态引用
 * 2) 与 zh-CN.ts 的 key 集合比对，找出缺失 key
 * 3) 校验 zh-CN / en-US 的 key 集合是否一致（防止只改一边）
 * 用法：node scripts/check-i18n.mjs
 */
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, extname } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'


/** 递归收集文件（排除 __tests__ 与 *.test.ts —— 测试 fixture 不属于源码） */
function walk(dir) {
  const out = []
  for (const name of readdirSync(dir)) {
    if (name === '__tests__' || name.endsWith('.test.ts')) continue
    const full = join(dir, name)
    if (statSync(full).isDirectory()) {
      out.push(...walk(full))
    } else if (extname(full) === '.vue' || extname(full) === '.ts') {
      out.push(full)
    }
  }
  return out
}

/** 从 i18n 语言目录提取全部 key（支持按模块拆分后的目录结构） */
function extractKeysFromLang(dirOrFile) {
  const stats = statSync(dirOrFile)
  if (stats.isFile()) {
    return extractKeysFromFile(dirOrFile)
  }
  // 目录结构：遍历所有 .ts 文件（排除 index.ts）
  const keys = new Set()
  for (const name of readdirSync(dirOrFile)) {
    if (name === 'index.ts' || !name.endsWith('.ts')) continue
    const subKeys = extractKeysFromFile(join(dirOrFile, name))
    subKeys.forEach((k) => keys.add(k))
  }
  return keys
}

function extractKeysFromFile(file) {
  const content = readFileSync(file, 'utf-8')
  const keys = new Set()
  const stack = []
  const lines = content.split('\n')
  for (const raw of lines) {
    const line = raw.trim()
    const m = line.match(/^([A-Za-z0-9_]+):\s*(.+)?$/)
    if (!m) continue
    const key = m[1]
    const rest = (m[2] || '').trim()
    const indent = raw.length - raw.trimStart().length
    while (stack.length && stack[stack.length - 1].indent >= indent) stack.pop()
    const fullPath = stack.length ? `${stack[stack.length - 1].path}.${key}` : key
    if (rest.startsWith('{')) {
      stack.push({ path: fullPath, indent })
    } else {
      keys.add(fullPath)
    }
  }
  return keys
}

/** 去掉行注释与块注释（文档示例 $t('xxx.yyy') 不算真实引用） */
function stripComments(content) {
  return content
    .replace(/\/\*[\s\S]*?\*\//g, ' ')
    .replace(/(^|[^:])'([^']*?)'\s*\/\/.*$/gm, (m, pre) => pre + "''")
    .split('\n')
    .map((line) => {
      const idx = line.indexOf('//')
      return idx === -1 ? line : line.slice(0, idx)
    })
    .join('\n')
}

/** 从代码中提取 $t('x.y.z') 静态引用 */
export function extractRefs(files) {
  const refs = new Set()
  const re = /\$t\(\s*'([^']+)'\s*\)/g
  for (const file of files) {
    const content = stripComments(readFileSync(file, 'utf-8'))
    let m
    while ((m = re.exec(content)) !== null) {
      // 排除动态拼接（含 ${ 或 + 的视为动态，跳过）
      if (m[1].includes('${') || m[1].includes('+')) continue
      refs.add(m[1])
    }
  }
  return refs
}

/**
 * M7：useFormDialog 的 i18nPrefix 命名空间必须含 add 键（新增弹窗标题用 ${prefix}.add）。
 * 提取所有 useFormDialog({ ... i18nPrefix: 'xxx' }) 的命名空间，校验 zh/en 均有 xxx.add。
 * @param {string[]} files 待扫描文件列表
 * @param {Set<string>} zhKeys zh-CN 全量 key 集合
 * @param {Set<string>} enKeys en-US 全量 key 集合
 * @returns {{ ns: string; file: string; line: number }[]}
 */
export function findI18nPrefixMissingAdd(files, zhKeys, enKeys) {
  const bad = []
  const re = /i18nPrefix:\s*'([^']+)'/g
  for (const file of files) {
    const content = readFileSync(file, 'utf-8')
    let m
    while ((m = re.exec(content)) !== null) {
      const ns = m[1]
      if (!zhKeys.has(`${ns}.add`) || !enKeys.has(`${ns}.add`)) {
        const line = content.slice(0, m.index).split('\n').length
        bad.push({ ns, file, line })
      }
    }
  }
  return bad
}

// ---- CLI 入口（被 vitest 导入时不执行） ----
const isMain = process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href
function runCli() {
  const src = join(fileURLToPath(new URL('../src', import.meta.url)))
  const files = walk(src)
  const refs = extractRefs(files)
  const zhKeys = extractKeysFromLang(join(src, 'i18n/lang/zh-CN'))
  const enKeys = extractKeysFromLang(join(src, 'i18n/lang/en-US'))

  let failed = false

  // 3) useFormDialog i18nPrefix 命名空间必须含 add 键
  const prefixMissing = findI18nPrefixMissingAdd(files, zhKeys, enKeys)
  if (prefixMissing.length) {
    failed = true
    console.error(`❌ ${prefixMissing.length} 个 useFormDialog i18nPrefix 命名空间缺少 add 键（新增弹窗标题将回退 common.create）:`)
    prefixMissing.forEach(({ ns, file, line }) => console.error(`   - ${ns}.add — ${file.replaceAll('\\', '/').replace(src.replaceAll('\\', '/'), 'src')}:${line}`))
  } else {
    console.log(`✅ useFormDialog i18nPrefix 命名空间均含 add 键`)
  }

  // 1) $t 引用缺失
  const missing = [...refs].filter((k) => !zhKeys.has(k)).sort()
  if (missing.length) {
    failed = true
    console.error(`❌ ${missing.length} 个 $t() 引用在 zh-CN 中缺失:`)
    missing.forEach((k) => console.error(`   - ${k}`))
  } else {
    console.log(`✅ ${refs.size} 个 $t() 静态引用全部存在于 zh-CN`)
  }

  // 2) zh-CN / en-US key 不一致
  const zhOnly = [...zhKeys].filter((k) => !enKeys.has(k)).sort()
  const enOnly = [...enKeys].filter((k) => !zhKeys.has(k)).sort()
  if (zhOnly.length || enOnly.length) {
    failed = true
    if (zhOnly.length) {
      console.error(`❌ ${zhOnly.length} 个 key 仅存在于 zh-CN:`)
      zhOnly.slice(0, 20).forEach((k) => console.error(`   - ${k}`))
    }
    if (enOnly.length) {
      console.error(`❌ ${enOnly.length} 个 key 仅存在于 en-US:`)
      enOnly.slice(0, 20).forEach((k) => console.error(`   - ${k}`))
    }
  } else {
    console.log(`✅ zh-CN / en-US key 完全一致（${zhKeys.size} 个）`)
  }

  if (failed) {
    process.exit(1)
  }
}

if (isMain) {
  runCli()
}