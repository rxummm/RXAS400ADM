#!/usr/bin/env bash
# ============================================================
# 部署到 AS400 服务器 US400CND
#
# 前置条件：
#   1. US400CND 上已安装 Java 17（Linux LPAR / PASE 环境）
#   2. 目标目录存在：/QOpenSys/opt/rxas400/{backend,frontend}
#   3. SSH 可登录（免密或密码）
#   4. 服务器上 MySQL 可访问，数据库 rxas400adm 已创建
#
# 用法：
#   bash scripts/deploy-to-as400.sh              # 构建并部署全部
#   bash scripts/deploy-to-as400.sh --backend    # 仅后端
#   bash scripts/deploy-to-as400.sh --frontend   # 仅前端
# ============================================================
set -e

AS400_HOST="${AS400_HOST:-US400CND}"
AS400_USER="${AS400_USER:-root}"
REMOTE_DIR="/QOpenSys/opt/rxas400"
JAR="rxas400adm-app-1.0.0-SNAPSHOT.jar"

cd "$(dirname "$0")/.."

deploy_backend() {
  echo "[后端] 构建..."
  (cd backend && mvn -q -DskipTests package)
  echo "[后端] 上传 $JAR 到 $AS400_HOST:$REMOTE_DIR/backend/"
  scp "backend/rxas400adm-app/target/$JAR" "$AS400_USER@$AS400_HOST:$REMOTE_DIR/backend/"
  echo "[后端] 上传启动脚本 start-backend.sh ..."
  scp scripts/start-backend.sh "$AS400_USER@$AS400_HOST:$REMOTE_DIR/backend/"
  echo "[后端] 重启服务..."
  ssh "$AS400_USER@$AS400_HOST" "bash $REMOTE_DIR/backend/start-backend.sh restart || true"
}

deploy_frontend() {
  echo "[前端] 构建..."
  (cd frontend && npm install --no-audit --no-fund && npm run build)
  echo "[前端] 上传 dist/ 到 $AS400_HOST:$REMOTE_DIR/frontend/"
  ssh "$AS400_USER@$AS400_HOST" "mkdir -p $REMOTE_DIR/frontend"
  scp -r frontend/dist/* "$AS400_USER@$AS400_HOST:$REMOTE_DIR/frontend/"
  echo "[前端] 完成（由 Nginx/Web 服务托管 $REMOTE_DIR/frontend）"
}

case "${1:-all}" in
  --backend) deploy_backend ;;
  --frontend) deploy_frontend ;;
  all) deploy_backend && deploy_frontend ;;
  *) echo "用法: $0 [--backend|--frontend]"; exit 1 ;;
esac

echo "部署完成 ✅"
