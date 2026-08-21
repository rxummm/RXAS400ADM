@echo off
rem Stop RXAS400 backend (8080) and frontend (5173) dev processes
chcp 65001 >nul
echo Stopping backend (8080)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1
echo Stopping frontend (5173)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173 ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1
echo Done.
