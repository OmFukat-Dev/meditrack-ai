@echo off
setlocal
cd /d "%~dp0"

echo ========================================
echo   MediTrack AI Full System Startup
echo ========================================
echo.

echo Starting Docker infrastructure...
call "%~dp0run-docker.bat"
if errorlevel 1 (
  echo ERROR: Docker startup failed.
  pause
  endlocal
  exit /b 1
)

echo.
echo Starting backend services...
call "%~dp0run-backend.bat" --skip-docker
if errorlevel 1 (
  echo ERROR: Backend startup failed.
  pause
  endlocal
  exit /b 1
)

echo.
echo Launching frontend...
start "MediTrack Frontend" cmd /k "%~dp0run-frontend.bat"

echo.
echo ========================================
echo   Startup commands issued successfully
echo ========================================
echo.
echo Frontend: http://localhost:3000
echo API Gateway: http://localhost:8090
echo Eureka Server: http://localhost:8761
echo.
echo If any service fails, inspect the corresponding CMD window for logs.
echo.
pause
endlocal
