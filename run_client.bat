@echo off
echo ==============================================
echo       Starting Booking Client (RMI)
echo ==============================================

set HOST=localhost
if not "%1"=="" set HOST=%1

echo Connecting to RMI Registry at host: %HOST%
java -cp "lib/*;bin" client.BookingClient %HOST%
pause
