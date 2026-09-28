@echo off
setlocal enabledelayedexpansion
title Distributed Tatkal Reservation System - Unified Launcher

echo ======================================================================
echo       DISTRIBUTED TATKAL RESERVATION SYSTEM - UNIFIED LAUNCHER
echo ======================================================================
echo.

set "ROOT_DIR=%~dp0"
set "FRONTEND_DIR=%ROOT_DIR%tatkal-frontend"

:: ----------------------------------------------------------------------
:: 1. CHECK NODE.JS
:: ----------------------------------------------------------------------
where node >nul 2>nul
if %errorlevel% neq 0 goto :err_no_node
echo [OK] Node.js detected:
node --version
goto :check_java

:err_no_node
echo.
echo [ERROR] Node.js is not installed or not in PATH!
echo Please install Node.js v18 or higher from https://nodejs.org/
pause
exit /b 1

:: ----------------------------------------------------------------------
:: 2. START JAVA RMI SERVER (Port 1099)
:: ----------------------------------------------------------------------
:check_java
echo.
echo [Checking Java RMI Booking Server...]
where java >nul 2>nul
if %errorlevel% neq 0 goto :skip_java

netstat -ano | findstr ":1099 " | findstr "LISTENING" >nul 2>nul
if %errorlevel% equ 0 (
    echo [OK] Java RMI Server is already running on port 1099.
    goto :check_frontend
)

echo [INFO] Starting Java RMI Server in background window...
start "Tatkal_RMI_Server" /min cmd /c "cd /d "%ROOT_DIR%" && java -cp "lib/*;bin" server.BookingServer"
ping -n 3 127.0.0.1 >nul
goto :check_frontend

:skip_java
echo [NOTICE] Java runtime not found. Next.js web frontend will communicate directly with the remote database.

:: ----------------------------------------------------------------------
:: 3. PREPARE & START NEXT.JS FRONTEND (Port 3000)
:: ----------------------------------------------------------------------
:check_frontend
echo.
echo [Checking Next.js Frontend...]
cd /d "%FRONTEND_DIR%"

if exist "node_modules" goto :check_build
echo [INFO] Installing frontend dependencies - one-time setup...
call npm install

:check_build
if exist ".next" goto :launch_web
echo [INFO] Building Next.js production package - one-time build...
call npm run build

:: ----------------------------------------------------------------------
:: 4. LAUNCH WEBSITE
:: ----------------------------------------------------------------------
:launch_web
echo.
echo ======================================================================
echo   READY! Starting server and opening website at:
echo   http://localhost:3000
echo   Connecting to REMOTE database (Supabase)
echo ======================================================================
echo.

start "" cmd /c "ping -n 3 127.0.0.1 >nul && start http://localhost:3000"
call npx next start -p 3000

pause
