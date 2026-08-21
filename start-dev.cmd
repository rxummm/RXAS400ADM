@echo off
rem ============================================================
rem  RXAS400 前后端一键快速启动
rem  - 自动停掉占用 8080 的旧后端
rem  - 后端用 mvn spring-boot:run（增量编译，免打 jar，启动快）
rem  - 前端用 Vite dev server
rem  访问 http://localhost:5173  （后端 API 文档 /swagger-ui.html）
rem ============================================================
chcp 65001 >nul
title RXAS400-Dev

echo [1/3] Stop old backend on 8080...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
)
timeout /t 1 /nobreak >nul

echo [2/3] Start backend (spring-boot:run)...
start "RXAS400-Backend" cmd /k "%~dp0backend\run-backend.cmd"

echo [3/3] Start frontend (vite)...
start "RXAS400-Frontend" cmd /k "%~dp0frontend\run-frontend.cmd"

echo.
echo Started. Open http://localhost:5173
echo Close the Backend/Frontend windows to stop, or run stop-dev.cmd
