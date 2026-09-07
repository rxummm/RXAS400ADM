#!/usr/bin/env node
/**
 * gen-fallback-i18n.mjs
 *
 * 从静态 TS 翻译文件生成 fallback JSON 到 src/i18n/fallback/。
 * 运行时 API 失败时，前端加载这些 JSON 作为降级翻译。
 *
 * 实现方式：用 new Function() 安全解析 TS 对象字面量，展平为 dot-notation JSON。
 *
 * 用法：node scripts/gen-fallback-i18n.mjs
 * 集成：package.json → "prebuild": "node scripts/gen-fallback-i18n.mjs"
 */
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve, join } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const srcDir = resolve(__dirname, '../src/i18n')
const fallbackDir = resolve(srcDir, 'fallback')

if (!existsSync(fallbackDir)) {
  mkdirSync(fallbackDir, { recursive: true })
}

/** 解析 TS export default { ... } 为 JS 对象 */
function parseTsToObject(filePath) {
  const content = readFileSync(filePath, 'utf-8')
  const match = content.match(/export\s+default\s+\{([\s\S]*)\}\s*$/m)
  if (!match) return {}
  try {
    return new Function(`return {${match[1]}}`)()
  } catch {
    console.warn(`⚠️  Failed to parse ${filePath}, skipping`)
    return {}
  }
}

/** 递归展平嵌套对象为 dot-notation: { a: { b: 'c' } } → { 'a.b': 'c' } */
function flattenObject(obj, prefix = '') {
  const result = {}
  for (const [key, value] of Object.entries(obj || {})) {
    const fullKey = prefix ? `${prefix}.${key}` : key
    if (typeof value === 'string') {
      result[fullKey] = value
    } else if (typeof value === 'object' && value !== null) {
      Object.assign(result, flattenObject(value, fullKey))
    }
  }
  return result
}

const locales = ['zh-CN', 'en-US']

for (const locale of locales) {
  const langDir = resolve(srcDir, `lang/${locale}`)
  const indexPath = join(langDir, 'index.ts')
  const merged = {}

  if (existsSync(indexPath)) {
    const indexContent = readFileSync(indexPath, 'utf-8')
    // 解析 import xxx from './yyy' 语句
    const importRegex = /import\s+(\w+)\s+from\s+['"]\.\/(\w+)['"]/g
    let importMatch
    while ((importMatch = importRegex.exec(indexContent)) !== null) {
      const tsFile = join(langDir, `${importMatch[2]}.ts`)
      if (existsSync(tsFile)) {
        const obj = parseTsToObject(tsFile)
        Object.assign(merged, flattenObject(obj))
      }
    }
  }

  const outPath = resolve(fallbackDir, `${locale}.json`)
  writeFileSync(outPath, JSON.stringify(merged, null, 2) + '\n', 'utf-8')
  console.log(`✅ ${locale}: ${Object.keys(merged).length} keys → ${outPath}`)
}

console.log('\nFallback i18n JSON generated.')
