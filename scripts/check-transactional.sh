#!/usr/bin/env bash
# =============================================================================
# check-transactional.sh —— @Transactional 零容忍扫描门禁
#
# 规则（§19.2 决策）：全库禁止任何 @Transactional 注解（含 rollbackFor 变体）。
#   R1  任何 @Transactional（裸注解或带属性变体）→ 违规
#   R2  任何 import org.springframework.transaction.* → 违规（连带清理）
#
# 背景（Trae 审计 §19 / AGENTS.md「事务与回滚」）：本项目主数据源为 MySQL 8
# （InnoDB 天然单语句原子性），AS400/DB2 for i 暂为只读（QSYS2 + 命令执行），
# 事务无收益反而引入 AS400 未开 Journaling 时的 SQL7008 回滚静默失效风险。
# 2026-08-15 已取消全部 @Transactional（21 文件 77 处），本门禁从零遗留起步，
# 无白名单——新增任何 @Transactional 即失败（exit 1）。
#
# 用法：bash scripts/check-transactional.sh [目标目录]
#   不带参数扫描 backend/；传参可指向任意目录（相对仓库根或绝对路径，供回归测试）。
# =============================================================================
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="${1:-backend}"
case "$TARGET" in
  /*) cd "$TARGET" || { echo "❌ 找不到目录 $TARGET"; exit 2; } ;;
  *)  cd "$ROOT/$TARGET" || { echo "❌ 找不到目录 $ROOT/$TARGET"; exit 2; } ;;
esac

FAIL=0

err()  { printf '  ✗  %s\n' "$1"; FAIL=1; }
info() { printf '  ✓  %s\n' "$1"; }

echo "== R1: 全库禁止任何 @Transactional（含 rollbackFor 变体）=="
# 命中所有 @Transactional 行，排除 import 与注释行
hits="$(grep -rn '@Transactional' --include='*.java' . | grep -v '/target/' \
        | grep -v 'import ' | grep -v '^\s*[^:]*:[0-9]*:\s*//' | grep -v '^\s*[^:]*:[0-9]*:\s*\*')"
if [ -n "$hits" ]; then
  while IFS= read -r line; do
    err "违规(@Transactional): $line"
  done <<<"$hits"
else
  info "全库 0 处 @Transactional（符合 §19.2 无事务架构）"
fi

echo "== R2: 禁止 import org.springframework.transaction.* =="
imp="$(grep -rn 'import org.springframework.transaction' --include='*.java' . | grep -v '/target/')"
if [ -n "$imp" ]; then
  while IFS= read -r line; do
    err "违规(import transaction): $line"
  done <<<"$imp"
else
  info "全库 0 处 org.springframework.transaction import"
fi

echo "----------------------------------------"
if [ "$FAIL" = "1" ]; then
  echo "❌ 事务注解检查未通过：发现 @Transactional / transaction import，请按 §19.2 移除"
  echo "   （本项目无事务架构，事务无收益且引入 AS400 未开 Journaling 的 SQL7008 风险）。"
  exit 1
fi
echo "✅ 事务注解检查通过（零容忍：全库 0 处 @Transactional）。"
exit 0