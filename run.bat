@echo off
cd /d "%~dp0"
where javac >nul 2>nul || (echo Java JDK 11 or newer is required. Install it from https://adoptium.net & pause & exit /b 1)
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
java -cp out hauntedmaze.Main
pause
