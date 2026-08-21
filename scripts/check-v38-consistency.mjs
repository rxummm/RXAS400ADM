#!/usr/bin/env node
/**
 * check-v38-consistency.mjs —— V38 种子 ↔ verify-fresh-db.sh V38_EXPECT_* 静态一致性校验
 *
 * 背景：M1 门禁 verify-fresh-db.sh 在【全新库 + MySQL】上断言结构计数 = V38 种子（V38_EXPECT_*）。
 * 本脚本不依赖 MySQL：直接解析 V38__seed_platform_structure.sql 的 INSERT 语句，静态算出
 * 角色/权限码/菜单（含 menu_type 分布）/角色绑定应产生的行数，与 verify-fresh-db.sh 里的
 * V38_EXPECT_* 逐一比对。任何一处漂移（改了 V38 没同步期望值、或期望值笔误）立刻失败。
 *
 * 用法：node scripts/check-v38-consistency.mjs
 * 接入：scripts/verify-all.sh + backend.yml（V38 seed static consistency）+ vitest 回归
 */
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')

const SH = readFileSync(join(ROOT, 'scripts/verify-fresh-db.sh'), 'utf-8')
const SQL = readFileSync(
  join(ROOT, 'backend/rxas400adm-app/src/main/resources/db/migration/V38__seed_platform_structure.sql'),
  'utf-8',
)

// ---------------- 1) 期望值（verify-fresh-db.sh 的 V38_EXPECT_*） ----------------
const num = (re) => Number((SH.match(re) || [])[1])
const expect = {
  menusTotal: num(/V38_EXPECT_MENUS_TOTAL=(\d+)/),
  menusByType: Object.fromEntries(
    (SH.match(/V38_EXPECT_MENUS_BY_TYPE="([^"]+)"/) || [])[1]
      ?.split(/\s+/)
      .filter(Boolean)
      .map((kv) => {
        const [k, v] = kv.split(':')
        return [Number(k), Number(v)]
      }) ?? [],
  ),
  perms: num(/V38_EXPECT_PERMS=(\d+)/),
  roles: num(/V38_EXPECT_ROLES=(\d+)/),
  rolePerms: num(/V38_EXPECT_ROLE_PERMS=(\d+)/),
  roleMenus: num(/V38_EXPECT_ROLE_MENUS=(\d+)/),
  roleCodes: ((SH.match(/V38_EXPECT_ROLE_CODES="([^"]+)"/) || [])[1] ?? '')
    .split(' ')
    .filter(Boolean)
    .sort(),
}
if (!expect.menusTotal) {
  console.error('❌ 无法从 scripts/verify-fresh-db.sh 解析 V38_EXPECT_*，检查文件内容')
  process.exit(2)
}

// ---------------- 2) 工具：逗号分割（尊重单引号） ----------------
function splitOuter(s) {
  const out = []
  let cur = ''
  let inQ = false
  for (const ch of s) {
    if (ch === "'") inQ = !inQ
    if (ch === ',' && !inQ) {
      out.push(cur)
      cur = ''
    } else {
      cur += ch
    }
  }
  if (cur.trim() !== '') out.push(cur)
  return out
}
const unquote = (v) => v.trim().replace(/^'|'$/g, '')

// ---------------- 3) 解析 V38 SQL ----------------
// 3.1 角色：INSERT IGNORE INTO rx_role (...) VALUES\n('ADMIN', ...),...;
const roleBlock = SQL.match(/INSERT IGNORE INTO rx_role \([^)]*\) VALUES\s*([\s\S]*?);/)?.[1] ?? ''
const roleRows = roleBlock.split('\n').filter((l) => l.trim().startsWith('('))
const roleCodes = roleRows
  .map((l) => unquote(splitOuter(l.trim().replace(/^\(/, '').replace(/\)[,;]?$/, ''))[0]))
  .sort()

// 3.2 权限码：INSERT IGNORE INTO rx_permission (...) VALUES\n(...),...;
const permBlock = SQL.match(/INSERT IGNORE INTO rx_permission \([^)]*\) VALUES\s*([\s\S]*?);/)?.[1] ?? ''
const permCount = permBlock.split('\n').filter((l) => l.trim().startsWith('(')).length

// 3.3 菜单：逐行 INSERT INTO rx_menu (cols) SELECT <tuple> WHERE NOT EXISTS (...);
const menus = []
for (const m of SQL.matchAll(/INSERT INTO rx_menu \(([^)]*)\)\s+SELECT\s+([^\n]+)\s+WHERE NOT EXISTS/g)) {
  const cols = splitOuter(m[1]).map(unquote)
  const vals = splitOuter(m[2].replace(/^SELECT\s*/, '')).map(unquote)
  const row = {}
  cols.forEach((c, i) => {
    row[c] = vals[i]
  })
  menus.push({ menuType: Number(row.menu_type), title: row.title, status: Number(row.status ?? 1) })
}
const menusByType = {}
for (const menu of menus) menusByType[menu.menuType] = (menusByType[menu.menuType] ?? 0) + 1

// 3.4 角色-权限：SELECT r.id, p.id ... WHERE r.role_code = 'ADMIN'（交叉连接 1 角色 × 全权限码）
const rpStmts = [...SQL.matchAll(/INSERT IGNORE INTO rx_role_permission[\s\S]*?WHERE r\.role_code = '([A-Z_]+)'/g)]
const rolePermCount = rpStmts.length * permCount

// 3.5 角色-菜单：4 条语句，ADMIN 全量 status=1 菜单，其余按 title IN (...)
const rmStmts = [
  ...SQL.matchAll(
    /INSERT IGNORE INTO rx_role_menu[\s\S]*?WHERE r\.role_code = '([A-Z_]+)' AND m\.status = 1([\s\S]*?);/g,
  ),
]
let roleMenuCount = 0
for (const m of rmStmts) {
  const titleList = [...m[2].matchAll(/'([^']+)'/g)].map((x) => x[1])
  const enabled = menus.filter((menu) => menu.status === 1)
  const matched = titleList.length === 0 ? enabled.length : enabled.filter((menu) => titleList.includes(menu.title)).length
  roleMenuCount += matched
}

// ---------------- 4) 断言 ----------------
let failed = false
const check = (name, actual, expected) => {
  if (actual === expected) {
    console.log(`✅ ${name} = ${actual}`)
  } else {
    console.error(`❌ ${name} = ${actual}（期望 ${expected}）`)
    failed = true
  }
}

check('rx_role 总数', roleRows.length, expect.roles)
check('rx_role 编码集合', roleCodes.join(','), expect.roleCodes.join(','))
check('rx_permission 总数', permCount, expect.perms)
check('rx_menu 总数', menus.length, expect.menusTotal)
for (const [type, count] of Object.entries(menusByType)) {
  check(`rx_menu menu_type=${type}`, count, expect.menusByType[Number(type)] ?? 0)
}
check('rx_menu 分布合计', Object.values(menusByType).reduce((a, b) => a + b, 0), expect.menusTotal)
check('rx_role_permission', rolePermCount, expect.rolePerms)
check('rx_role_menu', roleMenuCount, expect.roleMenus)

console.log('---')
if (failed) {
  console.error('❌ V38 种子与 verify-fresh-db.sh 的 V38_EXPECT_* 不一致：改了 V38 必须同步更新期望值，反之亦然')
  process.exit(1)
}
console.log('✅ V38 种子与 verify-fresh-db.sh V38_EXPECT_* 静态一致（无需 MySQL）')
