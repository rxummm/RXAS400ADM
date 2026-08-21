#!/usr/bin/env bash
# ============================================================
# RXAS400 后端启动脚本（适用于 AS400 PASE / Linux LPAR，无 systemd 时用 nohup）
# 部署路径：/QOpenSys/opt/rxas400/backend/
# ============================================================
set -e
cd "$(dirname "$0")"

APP_JAR="rxas400adm-app-1.0.0-SNAPSHOT.jar"
PID_FILE="rxas400.pid"
LOG_FILE="../logs/rxas400.log"
ENV_FILE="rxas400.env"
mkdir -p ../logs

# 若存在 rxas400.env（生产环境变量：JWT 密钥/数据库/采集间隔），自动加载
if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  . "./$ENV_FILE"
  set +a
fi

stop() {
  if [ -f "$PID_FILE" ]; then
    kill "$(cat "$PID_FILE")" 2>/dev/null || true
    rm -f "$PID_FILE"
    echo "已停止旧进程"
  fi
}

start() {
  stop
  # 生产环境必须设置环境变量（见部署文档）：
  #   RXAS400_JWT_SECRET / RXAS400_DB_HOST / RXAS400_DB_USER / RXAS400_DB_PASSWORD
  nohup java -jar "$APP_JAR" \
    --spring.profiles.active=prod \
    > "$LOG_FILE" 2>&1 &
  echo $! > "$PID_FILE"
  echo "后端已启动 (PID $(cat "$PID_FILE"))，日志: $LOG_FILE"
}

case "${1:-start}" in
  start) start ;;
  stop) stop ;;
  restart) start ;;
  status) [ -f "$PID_FILE" ] && ps -p "$(cat "$PID_FILE")" > /dev/null 2>&1 && echo "运行中 (PID $(cat "$PID_FILE"))" || echo "未运行" ;;
  *) echo "用法: $0 [start|stop|restart|status]"; exit 1 ;;
esac
