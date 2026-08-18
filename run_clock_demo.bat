@echo off
echo ==============================================
echo   Running Clock Algorithms Experiment (Exp 3)
echo ==============================================

set RMI_HOST=localhost
if not "%~1"=="" set RMI_HOST=%~1

java -cp "lib/*;bin" client.ClockAlgorithmsDemo %RMI_HOST%

echo.
pause
