#!/usr/bin/env bash
# =============================================================================
# scripts/verify-all.sh —— 根级一键门禁（本地提交前 / CI 复用）
#
# 聚合六道既有门禁，任何一道失败整体即失败（exit 1）：
#   1. scripts/check-layering.sh        后端分层准绳（Controller 禁 Mapper/QueryWrapper、DTO、VO）
#   2. scripts/check-frontend-slots.sh  前端 el-table 插槽行类型（裸 { row } / 悬空 RowType 即失败）
#   3. frontend CRLF 拼行防护            node scripts/check-template-join.mjs（并入 npm run lint）
#   4. 模板 class 样式定义                node scripts/check-template-classes.mjs（模板自定义 class 必须有 scoped/common.css 定义）
#   5. V38 种子静态一致性                  node scripts/check-v38-consistency.mjs（解析 V38 SQL 对比 V38_EXPECT_*，无需 MySQL）
#   6. 迁移结构一致性                     node scripts/check-migrations.mjs（V1~V42+ 序列/命名/对象去重/空迁移/MANIFEST，无需 MySQL）
#   7. 事务注解零容忍                     bash scripts/check-transactional.sh（全库禁止任何 @Transactional，§19.2，无需 MySQL）
#   8. i18n key 一致性 + i18nPrefix.add  node scripts/check-i18n.mjs（$t 引用存在 + zh/en 一致 + useFormDialog 前缀命名空间必含 add）
#   9. scripts/verify-fresh-db.sh       M1 全新库单源一致性（需 mysql 客户端 + 已打包 jar + 本地 MySQL）
#
# 用法：
#   bash scripts/verify-all.sh            # 全量（含第 5 项：全新库迁移校验，耗时约 2-3 分钟）
#   SKIP_DB=1 bash scripts/verify-all.sh  # 跳过第 5 项（本地快速门禁）
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

FAILED=0

run() { # run <名称> <命令...>
  local name="$1"; shift
  echo ""
  echo "========== $name =========="
  if "$@"; then
    echo "✅ [$name] 通过"
  else
    echo "❌ [$name] 失败"
    FAILED=1
  fi
}

run "后端分层准绳（check-layering）" bash scripts/check-layering.sh
run "前端插槽行类型（check-frontend-slots）" bash scripts/check-frontend-slots.sh
run "前端 CRLF 拼行防护（check-template-join）" bash -c 'cd frontend && node scripts/check-template-join.mjs'
run "前端模板 class 样式定义（check-template-classes）" bash -c 'cd frontend && node scripts/check-template-classes.mjs'
run "V38 种子静态一致性（check-v38-consistency，无需 MySQL）" node scripts/check-v38-consistency.mjs
run "迁移结构一致性（check-migrations，无需 MySQL）" node scripts/check-migrations.mjs
run "事务注解一致性（check-transactional，无需 MySQL）" bash scripts/check-transactional.sh
run "i18n key 一致性 + i18nPrefix.add 校验（check-i18n）" bash -c 'cd frontend && npm run check:i18n'

if [ "${SKIP_DB:-0}" = "1" ]; then
  echo ""
  echo "（SKIP_DB=1：跳过 M1 全新库校验）"
else
  run "M1 全新库单源一致性（verify-fresh-db，需本地 MySQL + 已打包 jar）" bash scripts/verify-fresh-db.sh
fi

echo ""
echo "========================================"
if [ "$FAILED" = "1" ]; then
  echo "❌ verify-all 未通过：存在失败门禁，修复后重跑。"
  exit 1
fi
if [ "${SKIP_DB:-0}" = "1" ]; then
  echo "🎉 verify-all 全绿（八道静态门禁通过，SKIP_DB=1 已跳过 M1 数据库校验）"
else
  echo "🎉 verify-all 全绿（九道门禁全部通过）"
fi
exit 0
