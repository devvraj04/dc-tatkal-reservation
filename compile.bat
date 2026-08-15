@echo off
echo ==============================================
echo       Compiling Distributed Booking System
echo ==============================================

:: Create directories if missing
if not exist lib mkdir lib
if not exist bin mkdir bin

:: Download PostgreSQL JDBC Driver if not present
if not exist lib\postgresql-42.7.3.jar (
    echo Downloading PostgreSQL JDBC Driver...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://jdbc.postgresql.org/download/postgresql-42.7.3.jar' -OutFile 'lib\postgresql-42.7.3.jar'"
    if errorlevel 1 (
        echo ERROR: Failed to download PostgreSQL JDBC Driver. Please download it manually from https://jdbc.postgresql.org/ and place it inside the 'lib' folder.
        pause
        exit /b 1
    )
    echo Download complete.
) else (
    echo PostgreSQL JDBC Driver found in lib/
)

:: Compile java files
echo Compiling source files...
javac -d bin -cp "lib/*;src" src/rmi/*.java src/server/*.java src/client/*.java src/com/tatkal/client/*.java
if errorlevel 1 (
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

echo Compilation successful! All classes compiled into 'bin/' directory.
echo ==============================================
pause
