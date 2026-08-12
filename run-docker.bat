@echo off
setlocal
cd /d "%~dp0"

set "PS_SCRIPT=%~dp0scripts\start-services.ps1"
if not exist "%PS_SCRIPT%" (
  echo ERROR: Missing startup script: %PS_SCRIPT%
  pause
  exit /b 1
)

echo Starting Docker infrastructure...
echo This will start MySQL, Redis, Kafka, and monitoring services.

echo Running PowerShell startup script...
PowerShell.exe -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%" -Mode DockerOnly
if errorlevel 1 (
  echo.
  echo Docker infrastructure failed to start.
  echo Make sure Docker Desktop is installed and running, then try again.
  pause
  endlocal
  exit /b 1
)

echo.
echo Docker infrastructure started successfully!
echo.
echo Services available at:
echo - MySQL: localhost:3306
echo - Redis: localhost:6379
echo - Kafka: localhost:9092
echo - Kafka UI: http://localhost:8095
echo - Prometheus: http://localhost:9090
echo - Grafana: http://localhost:3005
echo - Zipkin: http://localhost:9411
echo.
endlocal
