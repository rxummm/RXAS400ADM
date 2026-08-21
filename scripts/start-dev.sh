#!/usr/bin/env bash
# ============================================================
# RXAS400ADM 开发环境一键启动（后台）—— Git Bash / Linux
# 用法：
#   bash scripts/start-dev.sh          # 启动后端+前端并等待健康检查
#   bash scripts/start-dev.sh stop     # 停止
#   bash scripts/start-dev.sh status   # 查看状态
# 说明：mock 演示模式（无需真实 IBM i）；MySQL 需已启动（root/root）。
# ============================================================
set -euo pipefail
cd "$(dirname "$0")/.."

ROOT="$(pwd)"
LOG_DIR="$ROOT/logs"
JAR="backend/rxas400adm-app/target/rxas400adm-app-1.0.0-SNAPSHOT.jar"
BACKEND_LOG="$LOG_DIR/dev-backend.log"
FRONTEND_LOG="$LOG_DIR/dev-frontend.log"
BPID_FILE="$LOG_DIR/dev-backend.pid"
FPID_FILE="$LOG_DIR/dev-frontend.pid"
API_BASE="http://localhost:8080"
FRONT_URL="http://localhost:5173"
TIMEOUT=90   # 健康检查超时（秒）

mkdir -p "$LOG_DIR"

# ---------- 工具 ----------
alive() { # pid
  [ -n "${1:-}" ] && kill -0 "$1" 2>/dev/null
}

read_pid() { # file
  [ -f "$1" ] && cat "$1" 2>/dev/null || true
}

port_pid() { # port
  netstat -ano 2>/dev/null | grep -E ":$1 .*LISTEN" | awk '{print $NF}' | head -1
}

port_in_use() { # port
  [ -n "$(port_pid "$1")" ]
}

stop() {
  local p
  p="$(read_pid "$FPID_FILE")"
  if alive "$p"; then kill "$p" 2>/dev/null && echo "[前端] 已停止 (pid $p)"; fi
  p="$(read_pid "$BPID_FILE")"
  if alive "$p"; then kill "$p" 2>/dev/null && echo "[后端] 已停止 (pid $p)"; fi
  # 兜底：按端口杀（npm 是 shim，可能残留 node 子进程）
  p="$(port_pid 5173)"; [ -n "$p" ] && kill "$p" 2>/dev/null && echo "[前端] 已停止 (pid $p)"
  p="$(port_pid 8080)"; [ -n "$p" ] && kill "$p" 2>/dev/null && echo "[后端] 已停止 (pid $p)"
  rm -f "$BPID_FILE" "$FPID_FILE"
  echo "已停止。兜底：taskkill //F //IM java.exe + taskkill //F //IM node.exe"
}

status() {
  local bp fp
  bp="$(read_pid "$BPID_FILE")"; fp="$(read_pid "$FPID_FILE")"
  if alive "$bp"; then echo "后端: 运行中 (pid $bp)"; else echo "后端: 未运行"; fi
  if alive "$fp"; then echo "前端: 运行中 (pid $fp)"; else echo "前端: 未运行"; fi
  echo "后端端口: $(curl -s -o /dev/null -w '%{http_code}' "$API_BASE/api/v1/health" 2>/dev/null || echo 不可达)"
  echo "前端端口: $(curl -s -o /dev/null -w '%{http_code}' "$FRONT_URL/" 2>/dev/null || echo 不可达)"
}

# ---------- 启动 ----------
start_backend() {
  if [ ! -f "$JAR" ]; then
    echo "[后端] jar 不存在，执行构建 mvn -q -DskipTests package ..."
    (cd backend && mvn -q -DskipTests package)
  fi
  echo "[后端] 启动 $JAR ..."
  nohup java -jar "$JAR" --spring.datasource.password=root \
    > "$BACKEND_LOG" 2>&1 &
  echo $! > "$BPID_FILE"
  echo "[后端] PID $(cat "$BPID_FILE")，日志 $BACKEND_LOG"
}

wait_backend() {
  echo "[后端] 等待健康检查（登录接口，超时 ${TIMEOUT}s）..."
  local i code
  for i in $(seq 1 "$TIMEOUT"); do
    code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API_BASE/api/v1/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"username":"admin","password":"admin123"}' 2>/dev/null || true)
    if [ "$code" = "200" ]; then
      echo "[后端] ✅ 健康检查通过（${i}s）"
      return 0
    fi
    sleep 1
  done
  echo "[后端] ❌ 健康检查超时，日志：tail -50 $BACKEND_LOG"
  return 1
}

start_frontend() {
  echo "[前端] 启动 Vite dev server ..."
  (cd frontend && nohup npm run dev > "$FRONTEND_LOG" 2>&1 & echo $! > "$FPID_FILE")
  echo "[前端] PID $(cat "$FPID_FILE")，日志 $FRONTEND_LOG"
}

wait_frontend() {
  echo "[前端] 等待就绪（超时 60s）..."
  local i code
  for i in $(seq 1 60); do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$FRONT_URL/" 2>/dev/null || true)
    if [ "$code" = "200" ]; then
      echo "[前端] ✅ 就绪（${i}s）"
      return 0
    fi
    sleep 1
  done
  echo "[前端] ❌ 超时，日志：tail -50 $FRONTEND_LOG"
  return 1
}

# ---------- 入口 ----------
case "${1:-start}" in
  start)
    if port_in_use 8080 || port_in_use 5173; then
      echo "⚠️ 8080/5173 已被占用（可能服务已在运行）。先执行：bash scripts/start-dev.sh stop"
      status
      exit 1
    fi
    start_backend
    wait_backend
    start_frontend
    wait_frontend
    echo
    echo "============================================="
    echo "  前端  : http://localhost:5173   （admin / admin123）"
    echo "  后端  : http://localhost:8080   （Swagger /swagger-ui.html）"
    echo "  日志  : $LOG_DIR/dev-*.log"
    echo "  停止  : bash scripts/start-dev.sh stop"
    echo "============================================="
    ;;
  stop)  stop ;;
  status) status ;;
  *) echo "用法: $0 [start|stop|status]"; exit 1 ;;
esac
