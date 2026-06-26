@echo off
setlocal
cd /d "%~dp0"

echo ========================================
echo   MediTrack AI Complete System
echo ========================================
echo.
echo This will start:
echo 1. Docker Infrastructure (MySQL, Redis, Kafka, Monitoring)
echo 2. Backend Services (Eureka, Gateway, Microservices)
echo 3. Frontend Application (React App)
echo.
echo.

pause

echo Starting Docker Infrastructure...
call "%~dp0run-docker.bat"
if errorlevel 1 (
  echo.
  echo Docker infrastructure failed to start.
  endlocal
  exit /b 1
)

echo Starting Backend Services...
call "%~dp0run-backend.bat" --skip-docker
if errorlevel 1 (
  echo.
  echo Backend services failed to start.
  endlocal
  exit /b 1
)

echo Starting Frontend Application...
start "MediTrack Frontend" cmd /k ""%~dp0run-frontend.bat""

echo.
echo ========================================
echo   System Startup Complete!
echo ========================================
echo.
echo All components are starting in separate windows.
echo.
echo Access URLs:
echo - Frontend: http://localhost:3000
echo - API Gateway: http://localhost:8090
echo - Eureka Server: http://localhost:8761
echo - Grafana: http://localhost:3005 (admin/admin123)
echo - Kafka UI: http://localhost:8095
echo.
echo Close the individual windows to stop each service,
echo or run scripts\stop-services.ps1 to stop backend and Docker.
echo.
pause
endlocal
