#!/usr/bin/env bash
# ============================================================
# RXAS400 全量构建脚本：后端 jar + 前端 dist
# 用法：bash scripts/build.sh
# 输出：
#   backend/rxas400adm-app/target/rxas400adm-app-1.0.0-SNAPSHOT.jar
#   frontend/dist/
# ============================================================
set -e
cd "$(dirname "$0")/.."

echo "[1/3] 构建后端 (Maven)..."
cd backend
mvn -q -DskipTests package
cd ..

echo "[2/3] 构建前端 (Vite)..."
cd frontend
npm install --no-audit --no-fund
npm run build
cd ..

echo "[3/3] 产物清单："
ls -lh backend/rxas400adm-app/target/rxas400adm-app-*.jar
ls -lh frontend/dist/index.html
echo "构建完成 ✅"
