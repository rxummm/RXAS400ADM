#!/usr/bin/env bash
# ============================================================================
# scripts/verify-fresh-db.sh — M1 一致性校验（菜单/权限/角色单源门禁）
#
# 背景：8-13 报告 M1 指出「DataInitializer 与 Flyway 各维护一份菜单/权限结构」存在
# 双源漂移。修复方案是结构全量下沉 Flyway（V38 幂等种子），DataInitializer 只留
# 演示数据。本脚本在【全新数据库】上跑完全部迁移后，断言结构计数与
# （V38 种子 + 后置迁移增量）一致，防止回归：任何把结构播种写回代码、或
# V38 / 后置迁移被改动导致全新库漂移，CI 即失败。
#
# 用法：
#   bash scripts/verify-fresh-db.sh                          # 默认 root/root@127.0.0.1:3306
#   MYSQL_USER=root MYSQL_PWD=xxx JAR=path bash scripts/verify-fresh-db.sh
#
# CI：GitHub Actions backend.yml 已接入（MySQL 8 服务 + 本脚本）。
#
# ⚠️ 期望值 = V38__seed_platform_structure.sql 的全新库结果（已在 rxas400adm_fresh3 实测）。
#    若【有意】调整 V38 种子，请同步更新下列 V38_EXPECT_* 变量并重新实测确认。
# ============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PWD=${MYSQL_PWD:-root}
MYSQL_HOST=${MYSQL_HOST:-127.0.0.1}
MYSQL_PORT=${MYSQL_PORT:-3306}
JAR=${JAR:-backend/rxas400adm-app/target/rxas400adm-app-1.0.0-SNAPSHOT.jar}
APP_PORT=${APP_PORT:-18099}
DB=rxas400adm_fresh_verify

# ---- V38 种子期望值（V38__seed_platform_structure.sql 全新库结果） ----
# ⚠️ 本组变量必须与 V38 种子【静态一致】（check-v38-consistency.mjs 门禁会解析 V38 SQL 比对），
#    后置迁移新增的结构写在下方的 POST_V38_* 增量里，不要改这里。
V38_EXPECT_MENUS_TOTAL=90
V38_EXPECT_MENUS_BY_TYPE="1:8 2:44 3:23 4:15"     # 目录/叶子/按钮/Tab
V38_EXPECT_PERMS=55
V38_EXPECT_ROLES=4
V38_EXPECT_ROLE_PERMS=55
V38_EXPECT_ROLE_MENUS=197
V38_EXPECT_ROLE_CODES="ADMIN DEVELOPER OPERATOR VIEWER"

# ---- V38 之后的结构增量（后续迁移新增结构时同步更新：全量结构 = V38 种子 + 增量）----
# V47 flowcharts 叶子(type2)+1；V50 notifications 叶子(type2)+1；V58「AS400 业务」目录(type1)+1、叶子(type2)+1
POST_V38_MENUS_BY_TYPE="1:1 2:3"
# V58：BPCS_ORDER_VIEW 权限 +1（ADMIN 授权 +1）
# V56：COMPILE_EXECUTE 权限 -1（ADMIN 授权 -1）；compileExecute 按钮(menu_type3)-1 及其 role_menu-1
# → 权限/角色权限净 0；菜单净 type1+1/type2 +1(叶子)-1(按钮)=+1；role_menu 净 +2(目录+叶子)-1(按钮)=+1
POST_V38_PERMS=0
POST_V38_ROLE_PERMS=0
POST_V38_ROLE_MENUS=1

mysql=("mysql" "-u$MYSQL_USER" "-p$MYSQL_PWD" "-h$MYSQL_HOST" "-P$MYSQL_PORT" "--default-character-set=utf8mb4")

APP_PID=""
cleanup() {
  if [ -n "$APP_PID" ] && kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
  "${mysql[@]}" -e "DROP DATABASE IF EXISTS \`$DB\`;" 2>/dev/null || true
}
trap cleanup EXIT

log() { echo "[verify] $*"; }
fail() { echo "❌ $*"; exit 1; }

[ -f "$JAR" ] || fail "jar 不存在：$JAR（先执行 bash scripts/build.sh）"

log "创建全新库 $DB ..."
"${mysql[@]}" -e "DROP DATABASE IF EXISTS \`$DB\`; CREATE DATABASE \`$DB\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

log "启动应用（mock profile）执行全部迁移 ..."
java -jar "$JAR" \
  --server.port="$APP_PORT" \
  --spring.datasource.url="jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/$DB?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai" \
  --spring.datasource.password="$MYSQL_PWD" \
  --spring.profiles.active=mock \
  > /tmp/rxas_verify_fresh.log 2>&1 &
APP_PID=$!

# 等待最新迁移应用完成（轮询 flyway_schema_history 出现最新版本 success=1，最多 120s）
LATEST=$(ls backend/rxas400adm-app/src/main/resources/db/migration/V*.sql | sed -E 's#.*/V([0-9]+)__.*#\1#' | sort -n | tail -1)
[ -n "$LATEST" ] || fail "无法解析最新迁移版本号"
V_DONE=0
for _ in $(seq 1 120); do
  if "${mysql[@]}" -N -e "SELECT 1 FROM \`$DB\`.flyway_schema_history WHERE version='$LATEST' AND success=1 LIMIT 1;" 2>/dev/null | grep -q 1; then
    V_DONE=1
    break
  fi
  if ! kill -0 "$APP_PID" 2>/dev/null; then
    log "应用启动失败，日志尾部："
    tail -30 /tmp/rxas_verify_fresh.log || true
    exit 1
  fi
  sleep 1
done
[ "$V_DONE" = 1 ] || fail "超时：V$LATEST 未在 120s 内应用成功（日志：/tmp/rxas_verify_fresh.log）"
# 等 DataInitializer 演示数据与首次启动收尾，避免读到迁移半途状态
sleep 3

q() { "${mysql[@]}" -N -e "SELECT $1 FROM \`$DB\`.$2 $3" 2>/dev/null; }

# 全量菜单期望 = V38 种子 + 后置迁移增量（按 menu_type 合并）
exp_menu_total() {
  local total=0
  for kv in $POST_V38_MENUS_BY_TYPE; do total=$((total + ${kv##*:})); done
  echo "$((V38_EXPECT_MENUS_TOTAL + total))"
}
exp_menu_type() { # <type> -> 该类型全量期望（V38 种子 + 增量）
  local t="$1" base=0 post=0 k v
  for kv in $V38_EXPECT_MENUS_BY_TYPE; do
    k="${kv%%:*}"; v="${kv##*:}"
    [ "$k" = "$t" ] && base=$v
  done
  for kv in $POST_V38_MENUS_BY_TYPE; do
    k="${kv%%:*}"; v="${kv##*:}"
    [ "$k" = "$t" ] && post=$v
  done
  echo "$((base + post))"
}

# ---- 断言 ----
FAILED=0
check() { # check <名称> <实际> <期望>
  local name="$1" actual="$2" expected="$3"
  if [ "$actual" = "$expected" ]; then
    log "✅ $name = $actual"
  else
    echo "❌ $name = $actual（期望 $expected）"
    FAILED=1
  fi
}

check "rx_menu 总数"          "$(q 'COUNT(*)' rx_menu '')"                    "$(exp_menu_total)"
check "rx_menu 目录(1)"       "$(q 'COUNT(*)' rx_menu 'WHERE menu_type=1')"   "$(exp_menu_type 1)"
check "rx_menu 叶子(2)"       "$(q 'COUNT(*)' rx_menu 'WHERE menu_type=2')"   "$(exp_menu_type 2)"
check "rx_menu 按钮(3)"       "$(q 'COUNT(*)' rx_menu 'WHERE menu_type=3')"   "$(exp_menu_type 3)"
check "rx_menu Tab(4)"        "$(q 'COUNT(*)' rx_menu 'WHERE menu_type=4')"   "$(exp_menu_type 4)"
check "rx_permission 总数"    "$(q 'COUNT(*)' rx_permission '')"              "$((V38_EXPECT_PERMS + POST_V38_PERMS))"
check "rx_role 总数"          "$(q 'COUNT(*)' rx_role '')"                    "$V38_EXPECT_ROLES"
check "rx_role_permission"    "$(q 'COUNT(*)' rx_role_permission '')"         "$((V38_EXPECT_ROLE_PERMS + POST_V38_ROLE_PERMS))"
check "rx_role_menu"          "$(q 'COUNT(*)' rx_role_menu '')"               "$((V38_EXPECT_ROLE_MENUS + POST_V38_ROLE_MENUS))"

# 角色编码集合（空格分隔排序后比对）
ROLE_CODES_ACTUAL=$(q 'GROUP_CONCAT(role_code ORDER BY role_code SEPARATOR " ")' rx_role '')
check "rx_role 编码集合"      "$ROLE_CODES_ACTUAL" "$(echo "$V38_EXPECT_ROLE_CODES" | tr ' ' '\n' | sort | tr '\n' ' ' | sed 's/ $//')"

# 幂等性关键断言：菜单无 (parent_id,title,menu_type) 重复行（V38 靠 WHERE NOT EXISTS 防重复，
# 若将来双源回归或种子改坏，这里立刻暴露）
DUP_MENUS=$(mysql -u"$MYSQL_USER" -p"$MYSQL_PWD" -h"$MYSQL_HOST" -P"$MYSQL_PORT" -N --default-character-set=utf8mb4 \
  -e "SELECT COUNT(*) FROM (SELECT parent_id, title, menu_type FROM \`$DB\`.rx_menu GROUP BY parent_id, title, menu_type HAVING COUNT(*) > 1) t;" 2>/dev/null || true)
check "rx_menu 无重复行（幂等）" "${DUP_MENUS:-ERR}" "0"

if [ "$FAILED" = 1 ]; then
  fail "结构校验失败：全新库与 V38 种子不一致（如是有意修改 V38，请同步更新本脚本 V38_EXPECT_*）"
fi
log "🎉 M1 一致性校验通过：全新库结构完全由 V38 单源播种，无双源回归"
