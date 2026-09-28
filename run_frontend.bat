@echo off
echo ==============================================
echo       Starting Tatkal Next.js Frontend
echo ==============================================
cd /d "%~dp0tatkal-frontend"
start "" cmd /c "timeout /t 2 /nobreak >nul && start http://localhost:3000"
call npx next start -p 3000
pause
