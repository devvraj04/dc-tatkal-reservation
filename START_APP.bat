@echo off
setlocal enabledelayedexpansion
title Distributed Tatkal Reservation System - Unified Launcher

echo ======================================================================
echo       DISTRIBUTED TATKAL RESERVATION SYSTEM - UNIFIED LAUNCHER
echo ======================================================================
echo.

set "ROOT_DIR=%~dp0"
set "FRONTEND_DIR=%ROOT_DIR%tatkal-frontend"
set "PG_DATA=%ROOT_DIR%pg_data"
set "PG_PORT=5433"

:: ----------------------------------------------------------------------
:: 1. CHECK NODE.JS
:: ----------------------------------------------------------------------
where node >nul 2>nul
if %errorlevel% neq 0 goto :err_no_node
echo [OK] Node.js detected:
node --version
goto :check_postgres

:err_no_node
echo.
echo [ERROR] Node.js is not installed or not in PATH!
echo Please install Node.js v18 or higher from https://nodejs.org/
pause
exit /b 1

:: ----------------------------------------------------------------------
:: 2. DETECT & START POSTGRESQL
:: ----------------------------------------------------------------------
:check_postgres
echo.
echo [Checking PostgreSQL Database...]

:: Check if port 5433 is already active
netstat -ano | findstr ":5433 " | findstr "LISTENING" >nul 2>nul
if %errorlevel% equ 0 (
    echo [OK] PostgreSQL is already running on port 5433.
    set "PG_PORT=5433"
    goto :check_java
)

:: Check if default port 5432 is already active
netstat -ano | findstr ":5432 " | findstr "LISTENING" >nul 2>nul
if %errorlevel% equ 0 (
    echo [OK] Detected PostgreSQL running on standard port 5432.
    set "PG_PORT=5432"
    set "DATABASE_URL=postgresql://postgres@localhost:5432/postgres"
    goto :check_java
)

:: Search for postgres.exe across all standard Windows installation paths
set "PG_BIN="
if exist "C:\Program Files\PostgreSQL\18\bin\postgres.exe" set "PG_BIN=C:\Program Files\PostgreSQL\18\bin"
if not defined PG_BIN if exist "C:\Program Files\PostgreSQL\17\bin\postgres.exe" set "PG_BIN=C:\Program Files\PostgreSQL\17\bin"
if not defined PG_BIN if exist "C:\Program Files\PostgreSQL\16\bin\postgres.exe" set "PG_BIN=C:\Program Files\PostgreSQL\16\bin"
if not defined PG_BIN if exist "C:\Program Files\PostgreSQL\15\bin\postgres.exe" set "PG_BIN=C:\Program Files\PostgreSQL\15\bin"
if not defined PG_BIN if exist "C:\Program Files\PostgreSQL\14\bin\postgres.exe" set "PG_BIN=C:\Program Files\PostgreSQL\14\bin"
if not defined PG_BIN if exist "C:\Program Files\PostgreSQL\13\bin\postgres.exe" set "PG_BIN=C:\Program Files\PostgreSQL\13\bin"
if not defined PG_BIN if exist "C:\Program Files\PostgreSQL\12\bin\postgres.exe" set "PG_BIN=C:\Program Files\PostgreSQL\12\bin"
if not defined PG_BIN if exist "C:\PostgreSQL\bin\postgres.exe" set "PG_BIN=C:\PostgreSQL\bin"

if not defined PG_BIN (
    for /f "delims=" %%I in ('where postgres.exe 2^>nul') do (
        set "PG_BIN=%%~dpI"
    )
)

if not defined PG_BIN goto :err_no_pg

echo [OK] Found PostgreSQL installation at: %PG_BIN%

:: Initialize cluster if pg_data does not exist
if exist "%PG_DATA%\PG_VERSION" goto :start_pg

echo [INFO] Initializing database cluster at: %PG_DATA%
call "%PG_BIN%\initdb.exe" -D "%PG_DATA%" -U postgres -A trust -E UTF8
set "NEED_SEED=1"

:start_pg
echo [INFO] Starting PostgreSQL server on port 5433...
start "PostgreSQL_Server_5433" /min "%PG_BIN%\postgres.exe" -D "%PG_DATA%" -p 5433

:: Actively wait until PostgreSQL port 5433 is listening (up to 10 seconds)
set "WAIT_COUNT=0"
:wait_pg_loop
ping -n 2 127.0.0.1 >nul
netstat -ano | findstr ":5433 " | findstr "LISTENING" >nul 2>nul
if %errorlevel% equ 0 goto :pg_ready
set /a WAIT_COUNT+=1
if !WAIT_COUNT! geq 10 (
    echo [WARN] PostgreSQL took longer than expected to start on port 5433.
    goto :check_java
)
goto :wait_pg_loop

:pg_ready
echo [OK] PostgreSQL is confirmed running and listening on port 5433.
set "PG_PORT=5433"

if not defined NEED_SEED goto :check_java
echo [INFO] Seeding initial database schema from schema\schema.sql...
call "%PG_BIN%\psql.exe" -U postgres -h localhost -p 5433 -d postgres -f "%ROOT_DIR%schema\schema.sql"
call "%PG_BIN%\psql.exe" -U postgres -h localhost -p 5433 -d postgres -c "ALTER TABLE users ADD COLUMN IF NOT EXISTS is_leader BOOLEAN NOT NULL DEFAULT FALSE; UPDATE users SET is_leader = TRUE WHERE user_id = 1;"
goto :check_java

:err_no_pg
echo.
echo ======================================================================
echo [ERROR] PostgreSQL is not running and postgres.exe could not be found!
echo ======================================================================
echo Neither port 5433 nor port 5432 is currently active.
echo.
echo To run the Tatkal database:
echo 1. Ensure PostgreSQL is installed and running on port 5432 or 5433.
echo 2. Or configure DATABASE_URL in tatkal-frontend\.env.local.
echo 3. If PostgreSQL is installed in a custom directory, add its bin/ folder to PATH.
echo ======================================================================
echo.
pause
exit /b 1

:: ----------------------------------------------------------------------
:: 3. START JAVA RMI SERVER (Port 1099)
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
echo [NOTICE] Java runtime not found. Next.js web frontend will communicate directly with PostgreSQL.

:: ----------------------------------------------------------------------
:: 4. PREPARE & START NEXT.JS FRONTEND (Port 3000)
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
:: 5. LAUNCH WEBSITE
:: ----------------------------------------------------------------------
:launch_web
echo.
echo ======================================================================
echo   READY! Starting server and opening website at:
echo   http://localhost:3000
echo ======================================================================
echo.

start "" cmd /c "ping -n 3 127.0.0.1 >nul && start http://localhost:3000"
call npx next start -p 3000

pause
