@echo off
echo ==============================================
echo   Running Tatkal Multithreading Concurrency Test
echo ==============================================

set RMI_HOST=localhost
if not "%~1"=="" set RMI_HOST=%~1

java -cp "lib/*;bin" client.TatkalConcurrencyTest %RMI_HOST%

echo.
pause
