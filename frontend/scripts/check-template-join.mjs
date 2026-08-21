#!/usr/bin/env node
/**
 * check-template-join.mjs —— 防「CRLF 批量替换吞换行」回归（2026-08-15 实测教训）：
 *
 * 背景：对 CRLF 行尾的 .vue 文件做单行 oldString 批量替换时，会吞掉元素开标签与
 * `<template #default>` 之间的换行，把 `<el-table-column ...><template #default=...>`
 * 拼到同一行（24 处实测回归，被 lint 门禁 vue/multiline-html-element-content-newline 拦下）。
 *
 * 本脚本做一次快速定向扫描：任何 `.vue` 行内，`<el-* ...>` 开标签与 `<template #default`
 * 同处一行即违规（开标签未在该行闭合）。
 *
 * 用法：node scripts/check-template-join.mjs  （已并入 npm run lint）
 * 测试：findJoinedTemplateSlots 由 vitest 导入（src/__tests__/gates.test.ts）
 */
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, extname } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

/** 递归收集 .vue 文件 */
export function walk(dir) {
  const out = []
  for (const name of readdirSync(dir)) {
    const full = join(dir, name)
    if (statSync(full).isDirectory()) {
      out.push(...walk(full))
    } else if (extname(full) === '.vue') {
      out.push(full)
    }
  }
  return out
}

// 开标签 + 可选空白 + <template #default 同处一行（el-* 元素，开标签不含 > 内的 `>`）
const JOINED = /<el-[a-z-]+[^>]*>\s*<template #default/

/**
 * 检测一段 .vue 源码中的「开标签与 <template #default> 拼行」回归。
 * @param {string} content 文件内容（LF/CRLF 均可，按行检测）
 * @returns {{ line: number; text: string }[]}
 */
export function findJoinedTemplateSlots(content) {
  const bad = []
  const lines = content.split('\n')
  lines.forEach((line, i) => {
    if (JOINED.test(line)) {
      bad.push({ line: i + 1, text: line.trim().slice(0, 120) })
    }
  })
  return bad
}

// ---- CLI 入口（被 vitest 导入时不执行） ----
const isMain = process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href
if (isMain) {
  const SRC = join(fileURLToPath(new URL('../src', import.meta.url)))
  const files = walk(SRC)
  const bad = []
  for (const file of files) {
    const rel = file.replaceAll('\\', '/').replace(SRC.replaceAll('\\', '/'), 'src')
    const content = readFileSync(file, 'utf-8')
    for (const hit of findJoinedTemplateSlots(content)) {
      bad.push(`${rel}:${hit.line}: ${hit.text}`)
    }
  }

  if (bad.length) {
    console.error(`❌ 检测到 ${bad.length} 处元素开标签与 <template #default> 拼行（疑似 CRLF 批量替换吞换行）:`)
    bad.forEach((l) => console.error(`   - ${l}`))
    console.error('   请将 <template> 拆回独立行（保持与开标签同级缩进 +2）。')
    process.exit(1)
  }

  console.log(`✅ 无 el-* 开标签与 <template #default> 拼行（扫描 ${files.length} 个 .vue 文件）`)
}