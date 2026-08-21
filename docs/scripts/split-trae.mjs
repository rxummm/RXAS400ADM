#!/usr/bin/env node
/**
 * split-trae.mjs —— 把《Trae-RXAS400ADM-2026-08-15.md》按 `## ` 章标题拆成 VitePress 页面
 *
 * 规则：
 *  - 忽略第一张 `## ` 之前的前置区（标题/审计信息/章节摘要/可折叠目录，由 docs/index.md 承载）；
 *  - 跳过 `## 目录`（折叠 TOC 在站点上无意义，侧边栏已承担导航）；
 *  - 章节按中文数字映射为两位序号前缀（一→01 … 十八→18，附录→99），slug = 标题去标点；
 *  - 每个章节页 = frontmatter(title) + `# 章标题` + 原内容（verbatim，含代码块/表格/备注）。
 *
 * 用法：node scripts/split-trae.mjs   （在 docs/ 下执行，npm run split:trae）
 * 输出：docs/review/trae/{nn}-{slug}.md
 * 注意：源文件 CRLF 会被规范化为 LF（VitePress 无碍）；章节内容不做任何改写。
 */
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const DOCS = dirname(dirname(fileURLToPath(import.meta.url)))
const SRC = join(DOCS, 'Trae-RXAS400ADM-2026-08-15.md')
const OUT_DIR = join(DOCS, 'review', 'trae')

// 中文数字 → 两位序号（章节顺序固定，附录放最后）
const NUM = { 一: '01', 二: '02', 三: '03', 四: '04', 五: '05', 六: '06', 七: '07', 八: '08', 九: '09', 十: '10', 十一: '11', 十二: '12', 十三: '13', 十四: '14', 十五: '15', 十六: '16', 十七: '17', 十八: '18', 附录: '99' }

// ⚠️ 交替顺序：多字（十八…十一、附录）必须排在单字（十/九/…）之前，
// 否则「十一、」会被正则先匹配成「十」+ 空后缀，前缀误为 10-（本机实测坑）
const NUM_RE = /^(附录|十八|十七|十六|十五|十四|十三|十二|十一|十|九|八|七|六|五|四|三|二|一)[、：]?/

/** 章标题 → 文件名 slug：去掉序号前缀与标点，保留中文/字母/数字，空格转 - */
function slug(heading, num) {
  const rest = heading.replace(new RegExp(`^${num}、`), '').replace(/^附录：/, '附录-')
  const s = rest
    .toLowerCase()
    .replace(/[^\p{L}\p{N}\s-]/gu, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
  return `${NUM[num]}-${s}`
}

const raw = readFileSync(SRC, 'utf8').replace(/\r\n/g, '\n')
const parts = raw.split(/\n(?=## )/)
// parts[0] = 前置区（丢弃）；其后每段 = 一个 `## ` 章节

mkdirSync(OUT_DIR, { recursive: true })
const created = []
for (const part of parts.slice(1)) {
  const headingLine = part.split('\n')[0]
  const heading = headingLine.replace(/^## /, '').trim()
  if (heading === '目录') continue // 折叠 TOC 无需单独页

  const numMatch = heading.match(NUM_RE)
  const num = numMatch ? numMatch[1] : (heading.startsWith('附录') ? '附录' : null)
  if (!num) {
    console.warn(`  ⚠️ 跳过无编号章节: ${heading}`)
    continue
  }
  const file = `${slug(heading, num)}.md`
  const body = part.replace(/^## /, '# ').trimEnd() + '\n'
  const frontmatter = `---\ntitle: "${heading.replace(/"/g, '\\"')}"\n---\n\n`
  writeFileSync(join(OUT_DIR, file), frontmatter + body)
  created.push(file)
}

console.log(`✅ 生成 ${created.length} 个章节页 → ${OUT_DIR.replace(DOCS, 'docs')}`)
created.forEach((f) => console.log(`   - ${f}`))
