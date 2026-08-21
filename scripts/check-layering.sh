#!/usr/bin/env bash
# =============================================================================
# check-layering.sh —— 分层准绳扫描门禁（AGENTS.md「分层准绳（代码审查清单）」）
#
# 三条硬性规则：
#   R1  Controller 不得注入 Mapper、不得 new QueryWrapper / new LambdaQueryWrapper
#      （2026-08-16 已清零：原唯一例外 I18nController 也已下沉 II18nService，无白名单）
#   R2  新增写接口必须用 Create/Update DTO 接收 @RequestBody（禁止 Entity 当入参）
#   R3  新增返回接口优先复用既有 VO，禁止直接返 Entity
#
# 默认模式：已知遗留（历史 Admin-only CRUD / 例外页，见审查合订本
# 《CodeReview-RXAS400ADM-2026-08-14.md》第三部分 §二 与轮次 10/13）在 ALLOW_* 中放行，
# 任何**新增**文件/类型命中即失败（exit 1）——CI 使用默认模式。
# --strict 模式：连已知遗留也计为失败（用于专项整改清单，CI 不使用）。
#
# 用法：bash scripts/check-layering.sh [--strict] [目标目录]
#   不带参数扫描 backend/；传参可指向任意目录（相对仓库根或绝对路径，供回归测试）。
# =============================================================================
set -u

STRICT=0
[ "${1:-}" = "--strict" ] && { STRICT=1; shift; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="${1:-backend}"
case "$TARGET" in
  /*) cd "$TARGET" || { echo "❌ 找不到目录 $TARGET"; exit 2; } ;;
  *)  cd "$ROOT/$TARGET" || { echo "❌ 找不到目录 $ROOT/$TARGET"; exit 2; } ;;
esac

FAIL=0

# ---------------- 已知遗留放行清单（⚠️ 新增代码不得进入这些清单） ----------------
# R1：Controller 内直拼 Wrapper / 注入 Mapper 的既有文件——2026-08-16 已全量下沉 Service
#     （Health/Execution/AlertRule/Monitor/AuditLog/I18n/Auth/Config 共 8 个 Controller），
#     白名单清零：任何 Controller 直拼 Wrapper / 注入 Mapper（含历史遗留）即失败。
ALLOW_R1_WRAPPER_FILES=""
ALLOW_R1_MAPPER_FILES=""
# R2：@RequestBody 直接收 Entity 的既有类型——2026-08-16 已全量 DTO 化（16 个 Entity 入参
#     全部改为 *DTO），白名单清零：任何 Entity 入参（含历史遗留）即失败。
ALLOW_R2_ENTITY_TYPES=""
# R3：ApiResponse<Entity> 直返的既有类型（R2 全量 + 只读返 Entity 的两处；
#     SysConfig/SysPermission/SysMenu/SysRole/WebhookConfig/ReportSchedule/Notice/Region
#     CalendarEvent/DictType/DictItem/PermissionRequest 已 VO 化，移出白名单 2026-08-16）
ALLOW_R3_ENTITY_TYPES=""

# ---------------- 工具函数 ----------------
err()  { printf '  ✗  %s\n' "$1"; FAIL=1; }
info() { printf '  ✓  %s\n' "$1"; }

echo "== R1: Controller 不得 new QueryWrapper / 注入 Mapper =="
hits="$(grep -rn 'new LambdaQueryWrapper\|new QueryWrapper' --include='*Controller.java' . | grep -v '/target/')"
if [ -n "$hits" ]; then
  legacy=0
  while IFS= read -r line; do
    file="$(basename "$(echo "$line" | cut -d: -f1)")"
    if grep -qw "${file%.java}" <<<"$ALLOW_R1_WRAPPER_FILES"; then
      legacy=$((legacy + 1))
      [ "$STRICT" = "1" ] && err "R1 遗留(strict): $line"
    else
      err "R1 违规(新增文件): $line"
    fi
  done <<<"$hits"
  [ "$legacy" -gt 0 ] && [ "$STRICT" = "0" ] && printf '  ℹ  放行已知遗留 %d 处（I18nController 为例外页）\n' "$legacy"
else
  info "无 Controller 直拼 Wrapper"
fi

hits="$(grep -rn 'final .*Mapper' --include='*Controller.java' . | grep -v '/target/')"
if [ -n "$hits" ]; then
  legacy=0
  while IFS= read -r line; do
    file="$(basename "$(echo "$line" | cut -d: -f1)")"
    if grep -qw "${file%.java}" <<<"$ALLOW_R1_MAPPER_FILES"; then
      legacy=$((legacy + 1))
      [ "$STRICT" = "1" ] && err "R1 遗留(strict): $line"
    else
      err "R1 违规(新增文件): $line"
    fi
  done <<<"$hits"
  [ "$legacy" -gt 0 ] && [ "$STRICT" = "0" ] && printf '  ℹ  放行已知遗留 %d 处（Controller 注入 Mapper）\n' "$legacy"
else
  info "无 Controller 注入 Mapper"
fi

echo "== R2: @RequestBody 必须 DTO（禁 Entity 入参） =="
hits="$(grep -rn '@RequestBody' --include='*Controller.java' . | grep -v '/target/')"
if [ -n "$hits" ]; then
  legacy=0
  while IFS= read -r line; do
    type="$(echo "$line" | sed -nE 's/.*@RequestBody(\([^)]*\))?[[:space:]]+([A-Za-z][A-Za-z0-9_]*).*/\2/p')"
    [ -z "$type" ] && continue
    case "$type" in
      Map|List|String|Integer|Long|Boolean|byte) continue ;;
    esac
    case "$type" in
      *DTO|*Request|*VO) continue ;;
    esac
    if grep -qw "$type" <<<"$ALLOW_R2_ENTITY_TYPES"; then
      legacy=$((legacy + 1))
      [ "$STRICT" = "1" ] && err "R2 遗留(strict): $line"
    else
      err "R2 违规(新 Entity 入参): $line"
    fi
  done <<<"$hits"
  [ "$legacy" -gt 0 ] && [ "$STRICT" = "0" ] && printf '  ℹ  放行已知遗留 %d 处（Admin-only CRUD @RequestBody Entity）\n' "$legacy"
else
  info "无 @RequestBody"
fi

echo "== R3: 返回禁直接返 Entity（优先 VO） =="
# 合法返回类型白名单：基础类型 + 非 VO 后缀的值类型。
# *VO 后缀类型（VO 契约）自动合法、免登记——新增 VO 无需维护白名单（2026-08-16 加固，
# 此前 13 个新 VO 漏配 R3_OK_TYPES 导致门禁误报，见 gates.test.ts R3 回归用例）。
R3_OK_TYPES="Void String Boolean Integer Long List Map PageResult LoginResponse QueryResult CommandResult GraphData SysvalRow JobInfo ObjectDetail byte InputStream"
hits="$(grep -rn 'ApiResponse<' --include='*Controller.java' . | grep -v '/target/')"
if [ -n "$hits" ]; then
  legacy=0
  while IFS= read -r line; do
    type="$(echo "$line" | grep -oE 'ApiResponse<[A-Za-z][A-Za-z0-9_]*>' | head -1 | sed -E 's/ApiResponse<(.+)>/\1/')"
    [ -z "$type" ] && continue
    case "$type" in *VO) continue ;; esac   # *VO 后缀一律合法（VO 契约）
    grep -qw "$type" <<<"$R3_OK_TYPES" && continue
    if grep -qw "$type" <<<"$ALLOW_R3_ENTITY_TYPES"; then
      legacy=$((legacy + 1))
      [ "$STRICT" = "1" ] && err "R3 遗留(strict): $line"
    else
      err "R3 违规(新 Entity 返回): $line"
    fi
  done <<<"$hits"
  [ "$legacy" -gt 0 ] && [ "$STRICT" = "0" ] && printf '  ℹ  放行已知遗留 %d 处（Admin-only CRUD 直返 Entity）\n' "$legacy"
else
  info "无 ApiResponse 返回"
fi

echo "----------------------------------------"
if [ "$FAIL" = "1" ]; then
  echo "❌ 分层准绳检查未通过：存在违规（默认模式=新增违规；strict=含遗留）。"
  exit 1
fi
echo "✅ 分层准绳检查通过（默认模式：仅已知遗留，无新增违规）。"
exit 0