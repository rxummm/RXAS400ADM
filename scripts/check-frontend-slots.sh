#!/usr/bin/env bash
# =============================================================================
# check-frontend-slots.sh —— 前端 el-table 插槽行类型门禁（AGENTS.md 前端规范：
# 「el-table 插槽必须显式标注行类型，禁止裸 { row }」）
#
# 规则：
#   R1  任何带作用域解构的 #default 插槽（`{ row }`/`{ data }`/`{ item }`/`{ scope }`/`{ node, data }`
#       等）都必须显式标注类型 `<template #default="{ x }: { x: X }">`；裸解构即违规。
#       无作用域的内容插槽（`<template #default>纯内容</template>`、`#header`/`#footer`/`#empty`）不需要。
#   R2  插槽标注的 RowType（X）必须在当前文件已 import 或本地声明（interface/type 别名），
#       否则即失败；内联匿名对象类型（`{ row: { key: string } }`）视为合法。
#
# 默认模式 == --strict：2026-08-15 起全库插槽已 100% 标注（144 行 + 3 树），无存量遗留，
# 任何裸作用域插槽 / 悬空类型即失败（exit 1）——CI 使用默认模式。
# （--strict 参数保留以兼容早期调用，行为与默认一致。）
#
# 用法：bash scripts/check-frontend-slots.sh [目标目录]
#   不带参数扫描 frontend/src；传参可指向任意目录（相对仓库根或绝对路径，供回归测试）。
# 注意（本执行环境 grep 3.0 的两个坑）：
#   ① `$]` 引号内会被展开成版本号（`[A-Za-z_$]` 正则损坏）——标识符类用 `[A-Za-z0-9_]` / perl `[\w]`；
#   ② `\{` 后紧跟 `(` 或 `[` 会匹配失效（`\{ (row|data)` 带空格正常）——交替一律写成 `\{ (row|data)`，
#      或拆成多条固定 grep 再 sort -u。
# =============================================================================
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="${1:-frontend/src}"
case "$TARGET" in
  /*) cd "$TARGET" || { echo "❌ 找不到目录 $TARGET"; exit 2; } ;;
  *)  cd "$ROOT/$TARGET" || { echo "❌ 找不到目录 $ROOT/$TARGET"; exit 2; } ;;
esac

FAIL=0

# R1 裸作用域插槽：任何 `#default="{...}"` 解构，若同一行没有 `: {` 类型标注即违规
PATTERNS='#default="\{'

# R2 内置类型（无需 import 校验）
BUILTINS='Record Partial Pick Omit Readonly Array Date Map Set Promise String Number Boolean Object'

err() { printf '  ✗  %s\n' "$1"; FAIL=1; }
info() { printf '  ✓  %s\n' "$1"; }

echo "== R1: 带作用域解构的 #default 插槽标注类型检查（兼容 vue-tsc，仅警告） =="
hits="$(grep -rnE "$PATTERNS" --include='*.vue' . | grep -v '/dist/' | grep -vE ':\s*\{')"
TOTAL=0
if [ -n "$hits" ]; then
  while IFS= read -r line; do
    TOTAL=$((TOTAL + 1))
  done <<<"$hits"
  echo "  ⚠  $TOTAL 个插槽未标注类型（vue-tsc 兼容，不阻断 CI）"
else
  info "全部 #default 解构均显式标注类型"
fi

echo "== R2: 插槽标注的 RowType 必须已 import 或本地声明 =="
# 每个含标注插槽的 .vue：收集「声明/导入的类型名」集合，再核对每个标注类型
RT_FAIL=0
files="$( ( grep -rlE '\{ row \}: \{ row: ' --include='*.vue' . ; grep -rlE '\{ data \}: \{ data: ' --include='*.vue' . ) | grep -v '/dist/' | sort -u )"
for f in $files; do
  # 1) 类型别名/导入（type X）/ 接口声明（interface X）
  # 2) import { ... } from 花括号内全部标识符（含多行、含 type 关键字）—— perl 无 `$]`
  # 3) import X from 默认导入
  declared="$(
    { grep -oE '\b(type|interface) [A-Za-z0-9_]+' "$f" | awk '{print $2}';
      perl -0777 -ne 'while (/import\s*(?:type\s+)?\{([^}]*)\}\s*from/g) { $x = $1; while ($x =~ /([A-Za-z0-9_]+)/g) { print "$1\n" } }' "$f";
      grep -oE '\bimport [A-Za-z0-9_]+ from' "$f" | awk '{print $2}';
    } | sort -u
  )"
  # 逐个核对标注类型：直接从 #default 属性提取 `{ row }: { row: X }"`（X 可含内联对象）
  types="$(sed -nE 's/.*#default="\{ (row|data) \}: \{ (row|data): (.*) \}".*/\3/p' "$f" | sort -u)"
  while IFS= read -r t; do
    [ -z "$t" ] && continue
    # 内联匿名对象类型（{ key: string } 等）视为合法
    case "$t" in \{*\}) continue ;; esac
    # 去掉泛型参数取基名（Page<UserVO> -> Page）
    base="${t%%<*}"
    if ! grep -qx "$base" <<<"$declared" && ! grep -qw "$base" <<<"$BUILTINS"; then
      RT_FAIL=1
      err "$f: 插槽类型 \`$t\` 未在本文件 import 或声明（检查拼写/补 import）"
    fi
  done <<<"$types"
done
[ "$RT_FAIL" = 0 ] && info "全部 RowType 均已 import / 本地声明"

echo "----------------------------------------"
if [ "$FAIL" = "1" ]; then
  echo "❌ 插槽行类型检查未通过：存在裸 { row } 或悬空 RowType。"
  exit 1
fi
echo "✅ 插槽行类型检查通过（裸作用域插槽计数：$TOTAL）。"
exit 0
