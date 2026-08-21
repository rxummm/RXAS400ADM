@echo off
REM ============================================================
REM RXAS400ADM dev one-click launcher (background) - Windows cmd
REM Usage:
REM   start-dev.bat           start backend + frontend, wait health
REM   start-dev.bat stop      stop both
REM   start-dev.bat status    show status
REM Requires: MySQL running (root/root), java & mvn & node on PATH.
REM ============================================================
setlocal EnableDelayedExpansion
chcp 65001 >nul
cd /d "%~dp0.."

set "ROOT=%CD%"
set "LOG_DIR=%ROOT%\logs"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

set "JAR=backend\rxas400adm-app\target\rxas400adm-app-1.0.0-SNAPSHOT.jar"
set "BACKEND_LOG=%LOG_DIR%\dev-backend.log"
set "FRONTEND_LOG=%LOG_DIR%\dev-frontend.log"
set "BPID_FILE=%LOG_DIR%\dev-backend.pid"
set "FPID_FILE=%LOG_DIR%\dev-frontend.pid"

if "%1"=="stop"   goto :stop
if "%1"=="status" goto :status

REM ---- port check ----
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>&1 && (
  echo [ERR] Port 8080 already in use. Run: start-dev.bat stop
  goto :status
)
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul 2>&1 && (
  echo [ERR] Port 5173 already in use. Run: start-dev.bat stop
  goto :status
)

REM sleep helper: ping works even when stdout is redirected/inside pipes
REM (timeout.exe fails with "Input redirection is not supported" in that case;
REM  GNU timeout may shadow it when Git Bash is on PATH). ~1s per ping -n 2.
set "SLEEP=ping -n 2 127.0.0.1 >nul"

REM ---- backend ----
if not exist "%JAR%" (
  echo [BACKEND] Building jar...
  pushd backend
  call mvn -q -DskipTests package
  popd
)
echo [BACKEND] Starting...
powershell -NoProfile -Command "(Start-Process -FilePath 'java.exe' -ArgumentList '-jar','%JAR%','--spring.datasource.password=root' -WorkingDirectory '%ROOT%' -RedirectStandardOutput '%BACKEND_LOG%' -RedirectStandardError '%BACKEND_LOG%.err' -WindowStyle Hidden -PassThru).Id" > "%BPID_FILE%"
echo [BACKEND] PID %BPID_FILE% , log %BACKEND_LOG%

REM ---- wait backend health ----
echo [BACKEND] Waiting for health check (timeout 90s)...
set /a N=0
:wait_backend
set /a N+=1
if !N! GEQ 90 goto :backend_timeout
curl -s -o nul -w "%%{http_code}" -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}" > "%LOG_DIR%\hc.tmp" 2>nul
set /p HC=<"%LOG_DIR%\hc.tmp"
if "!HC!"=="200" (
  echo [BACKEND] OK after !N!s
  goto :frontend
)
%SLEEP%
goto :wait_backend
:backend_timeout
echo [ERR] Backend health check timeout. See %BACKEND_LOG%
exit /b 1

REM ---- frontend ----
:frontend
echo [FRONTEND] Starting Vite...
powershell -NoProfile -Command "(Start-Process -FilePath 'npm.cmd' -ArgumentList 'run','dev' -WorkingDirectory '%ROOT%\frontend' -RedirectStandardOutput '%FRONTEND_LOG%' -RedirectStandardError '%FRONTEND_LOG%.err' -WindowStyle Hidden -PassThru).Id" > "%FPID_FILE%"
echo [FRONTEND] PID %FPID_FILE% , log %FRONTEND_LOG%

set /a N=0
:wait_front
set /a N+=1
if !N! GEQ 60 goto :front_timeout
curl -s -o nul -w "%%{http_code}" http://localhost:5173/ > "%LOG_DIR%\hc2.tmp" 2>nul
set /p HC2=<"%LOG_DIR%\hc2.tmp"
if "!HC2!"=="200" (
  echo [FRONTEND] OK after !N!s
  goto :done
)
%SLEEP%
goto :wait_front
:front_timeout
echo [ERR] Frontend timeout. See %FRONTEND_LOG%
exit /b 1

:done
echo.
echo =============================================
echo   Frontend : http://localhost:5173   (admin / admin123)
echo   Backend  : http://localhost:8080   (Swagger /swagger-ui.html)
echo   Logs     : %LOG_DIR%\dev-*.log
echo   Stop     : start-dev.bat stop
echo =============================================
exit /b 0

:stop
if exist "%BPID_FILE%" (
  set /p BP=<"%BPID_FILE%"
  taskkill /F /PID !BP! >nul 2>&1 && echo [BACKEND] stopped (pid !BP!)
)
if exist "%FPID_FILE%" (
  set /p FP=<"%FPID_FILE%"
  taskkill /F /PID !FP! >nul 2>&1 && echo [FRONTEND] stopped (pid !FP!)
)
REM fallback: kill by port (npm shim may leave node child)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do taskkill /F /PID %%p >nul 2>&1 && echo [BACKEND] stopped (pid %%p)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') do taskkill /F /PID %%p >nul 2>&1 && echo [FRONTEND] stopped (pid %%p)
del /q "%BPID_FILE%" "%FPID_FILE%" >nul 2>&1
echo Stopped. Fallback: taskkill /F /IM java.exe + taskkill /F /IM node.exe
exit /b 0

:status
set "BP=" & set "FP="
if exist "%BPID_FILE%" set /p BP=<"%BPID_FILE%"
if exist "%FPID_FILE%" set /p FP=<"%FPID_FILE%"
echo Backend PID: %BP%   Frontend PID: %FP%
curl -s -o nul -w "Backend  :8080 - HTTP %%{http_code}" http://localhost:8080/api/v1/health 2>nul & echo.
curl -s -o nul -w "Frontend :5173 - HTTP %%{http_code}" http://localhost:5173/ 2>nul & echo.
exit /b 0
