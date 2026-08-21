@echo off
rem ============================================================
rem  RXAS400 后端快速启动（增量编译，免打包）
rem  1) 先 install 全部模块到本地 Maven 仓库（保证 app 能拿到
rem     system/security 等模块的最新类）
rem  2) spring-boot:run 启动 app 模块
rem ============================================================
chcp 65001 >nul
title RXAS400-Backend
cd /d "%~dp0"
echo [Backend] mvn install (all modules -> local repo)...
call mvn -q -DskipTests install || goto :err
cd /d "%~dp0rxas400adm-app"
echo [Backend] mvn spring-boot:run ...
mvn spring-boot:run -Dspring-boot.run.jvmArguments=-Dfile.encoding=UTF-8
exit /b

:err
echo.
echo [ERROR] mvn install 失败，请先修复编译错误。
pause
