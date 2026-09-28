@echo off
echo ========================================================
echo       STARTING TATKAL REPLICATION EXPERIMENT
echo ========================================================

if not exist bin\com\tatkal\test\ReplicationTest.class (
    echo ERROR: Compiled classes not found.
    echo Please run compile.bat first.
    pause
    exit /b 1
)

echo Starting Replication Demo...
java -cp "lib/*;bin" com.tatkal.test.ReplicationTest

pause
