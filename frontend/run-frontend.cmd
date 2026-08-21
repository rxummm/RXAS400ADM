@echo off
rem ============================================================
rem  RXAS400 前端快速启动（Vite dev server）
rem  访问 http://localhost:5173 ，代理 /api 与 /ws 到 8080
rem ============================================================
chcp 65001 >nul
title RXAS400-Frontend
cd /d "%~dp0frontend"
echo [Frontend] npm run dev ...
npm run dev
