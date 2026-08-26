#!/usr/bin/env node
/**
 * check-migrations.mjs —— Flyway 迁移文件结构一致性校验（V1~V42 + 未来新增）
 *
 * 校验四件事：
 *   1. 序列：V{n}__desc.sql 命名合法、版本连续无缺号、无重复版本（分支合并常见事故）；
 *   2. 对象去重：跨全部迁移，CREATE TABLE 表名、CREATE/ADD INDEX 的 (表, 索引名) 不得重复
 *      （两个分支各自建同名索引/表的合并冲突，静态即可暴露）；
 *   3. MANIFEST 断言：文档化承诺的对象必须真实出现在对应迁移（如下表）——
 *      V40 唯一索引 uk_ibmi_system_name / V41 idx_metric_name_time / V42 idx_metric_instance_time，
 *      均来自审查合订本轮次 12（P3）的落地描述；
 *   4. 空迁移：每个迁移必须含至少一条非注释语句（防误提交空文件）。
 *
 * 【新增迁移固定模板】（参照下述约定，写完跑本脚本 + verify-fresh-db.sh）：
 *   - 命名：V{n}__短横线描述.sql，n = 当前最大版本 + 1（严禁跳号/重号）；
 *   - 幂等：种子用 INSERT IGNORE / WHERE NOT EXISTS；结构变更用 information_schema 预查 +
 *     PREPARE 动态 DDL（参考 V40/V41/V42）；存储过程必须包 DELIMITER $$ ... $$；
 *   - 若创建了会在 review/文档中被断言的对象（唯一索引/关键索引/新表），在下方 MANIFEST
 *     补一行 { 版本: ['对象名', ...] }；
 *   - 提交前：node scripts/check-migrations.mjs && bash scripts/verify-fresh-db.sh（或 SKIP_DB=1）。
 *
 * 用法：node scripts/check-migrations.mjs
 * 接入：scripts/verify-all.sh + backend.yml（Migration structure consistency）+ vitest 回归
 */
import { readFileSync, readdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const MIG_DIR = join(ROOT, 'backend/rxas400adm-app/src/main/resources/db/migration')

// 文档化对象断言：版本 → 该迁移必须包含的对象 token（来自审查合订本轮次 12 P3 描述 + M1 结构化状态）
const MANIFEST = {
  40: ['uk_ibmi_system_name'],
  41: ['idx_metric_name_time'],
  42: ['idx_metric_instance_time'],
  44: ['last_run_status'],
  55: ['upgrade_notified', 'idx_alert_event_upgrade'],
}

// 已知历史遗留（⚠️ 新增重复不得进此清单）：V29/V30 同为 cleanup_deploy_and_scheduler_lock，
// 内容几乎一致（CREATE TABLE IF NOT EXISTS + INSERT IGNORE），运行时幂等安全；
// V30 已被既有库 Flyway 历史记录，不可删除，故静态放行该具体配对。
const ALLOW_TABLE_DUPES = new Map([
  ['rx_scheduler_lock', new Set(['V29__cleanup_deploy_and_scheduler_lock.sql', 'V30__cleanup_deploy_and_scheduler_lock.sql'])],
])

const files = readdirSync(MIG_DIR).filter((f) => f.endsWith('.sql')).sort()

let failed = false
const check = (name, ok, detail = '') => {
  if (ok) {
    console.log(`✅ ${name}${detail ? ` ${detail}` : ''}`)
  } else {
    console.error(`❌ ${name}${detail ? ` ${detail}` : ''}`)
    failed = true
  }
}

// ---------------- 1) 序列/命名 ----------------
const versions = []
const bad = []
for (const f of files) {
  const m = f.match(/^V(\d+)__.+\.sql$/)
  if (!m) {
    bad.push(`${f}（命名不合法，应为 V{n}__desc.sql）`)
    continue
  }
  versions.push(Number(m[1]))
}
check(`迁移序列：V1~V${versions.length} 共 ${versions.length} 个文件，版本连续唯一`, true, bad.length ? `⚠️ ${bad.join('、')}` : '')
for (const b of bad) console.error(`   - ${b}`)
if (bad.length) failed = true

const uniq = [...new Set(versions)].sort((a, b) => a - b)
check('版本号无重复', uniq.length === versions.length)
check('版本连续无缺号', uniq.every((v, i) => v === i + 1), `（${uniq.join(',')}）`)

// ---------------- 工具 ----------------
/** 去掉注释行 + DELIMITER 过程块，按 `;` 拆分语句（尊重单双引号） */
function splitStatements(sql) {
  const lines = sql.split('\n').filter((l) => !l.trim().startsWith('--'))
  let text = lines.join('\n')
  // 移除 DELIMITER $$ ... $$ 过程块（含内部多条语句，避免误拆分）
  text = text.replace(/DELIMITER\s+\$\$[\s\S]*?\$\$\s*DELIMITER\s*;/gi, ' /*proc*/;')
  const stmts = []
  let cur = ''
  let inS = false
  let inD = false
  for (const ch of text) {
    if (ch === "'" && !inD) inS = !inS
    if (ch === '"' && !inS) inD = !inD
    if (ch === ';' && !inS && !inD) {
      if (cur.trim()) stmts.push(cur.trim())
      cur = ''
    } else {
      cur += ch
    }
  }
  if (cur.trim()) stmts.push(cur.trim())
  return stmts
}

const tables = new Map() // table -> first seen file
const indexes = new Map() // `${table}.${index}` -> first seen file
const seenStmts = new Map() // file -> stmt count

for (const f of files) {
  const sql = readFileSync(join(MIG_DIR, f), 'utf-8')
  const stmts = splitStatements(sql)
  seenStmts.set(f, stmts.length)

  for (const stmt of stmts) {
    const table = stmt.match(/^CREATE TABLE\s+(?:IF NOT EXISTS\s+)?`?(\w+)`?/i)
    if (table) {
      const dupAllowed = ALLOW_TABLE_DUPES.get(table[1])?.has(f)
      if (tables.has(table[1])) {
        if (dupAllowed) {
          console.log(`  ℹ  放行已知遗留：\`${table[1]}\` 在 ${tables.get(table[1])} 与 ${f} 重复（IF NOT EXISTS 幂等，历史遗留不可删）`)
        } else {
          check('表名跨迁移不重复', false, `\`${table[1]}\` 重复创建（${tables.get(table[1])} 与 ${f}）`)
        }
      } else {
        tables.set(table[1], f)
      }
    }
    const idx = stmt.match(/^(?:CREATE (?:UNIQUE )?INDEX|ALTER TABLE\s+`?(\w+)`?\s+ADD (?:UNIQUE )?(?:INDEX|KEY))\s+`?(\w+)`?/i)
    if (idx) {
      const tbl = idx[1] ?? (stmt.match(/ON\s+`?(\w+)`?/i) || [])[1]
      const key = `${tbl}.${idx[2]}`
      if (tbl && indexes.has(key)) {
        check('索引跨迁移不重复', false, `${key} 重复创建（${indexes.get(key)} 与 ${f}）`)
      } else if (tbl) {
        indexes.set(key, f)
      }
    }
  }
}
check('表/索引跨迁移对象不重复', !failed)

// ---------------- 2) 空迁移 ----------------
// 已知例外：V31 为注释型占位迁移（AES 加密说明，无 DDL，Flyway no-op，历史遗留不可删）
const ALLOW_EMPTY = new Set([31])
for (const f of files) {
  const v = Number(f.match(/^V(\d+)/)?.[1])
  const ok = (seenStmts.get(f) ?? 0) > 0 || ALLOW_EMPTY.has(v)
  check(`非空迁移（${f}）${ALLOW_EMPTY.has(v) ? '（已知占位例外）' : ''}`, ok)
}

// ---------------- 3) MANIFEST 断言 ----------------
for (const [ver, tokens] of Object.entries(MANIFEST)) {
  const file = files.find((f) => f.startsWith(`V${ver}__`))
  if (!file) {
    check(`MANIFEST V${ver} 文件存在`, false)
    continue
  }
  const content = readFileSync(join(MIG_DIR, file), 'utf-8')
  const stripped = content.split('\n').filter((l) => !l.trim().startsWith('--')).join('\n')
  for (const tok of tokens) {
    check(`V${ver} 文档化对象 \`${tok}\` 在迁移中`, stripped.includes(tok))
  }
}

// ---------------- 汇总 ----------------
console.log('---')
const indexTotal = indexes.size
const tableTotal = tables.size
console.log(`（共 ${files.length} 个迁移：${tableTotal} 张表、${indexTotal} 个索引对象被创建）`)
if (failed) {
  console.error('❌ 迁移结构一致性未通过：修复后重跑；新增迁移请按文件头「固定模板」约定。')
  process.exit(1)
}
console.log('✅ 迁移结构一致性通过（序列/命名/对象去重/空迁移/MANIFEST 断言）')
