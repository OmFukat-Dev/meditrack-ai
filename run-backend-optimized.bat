@echo off
setlocal enabledelayedexpansion

REM Start backend services with reduced heap sizes to avoid OOM on 7GB system
REM Services run sequentially in separate windows; adjust -Xmx values as needed

set HEAP_SIZE=-Xmx384m
set JAR_DIR=d:\Projects\CascadeProjects\meditrack-ai\backend

echo.
echo ====================================================
echo MediTrack AI Backend Services - Optimized Start
echo Heap Size per service: 384MB (total ~3GB for 8 services)
echo ====================================================
echo.

REM Eureka Server (Service Registry)
echo Starting Eureka Server on port 8761...
start "Eureka Server" cmd /k "cd /d %JAR_DIR%\eureka-server && java %HEAP_SIZE% -jar target\eureka-server-1.0.0.jar"
timeout /t 5

REM API Gateway
echo Starting API Gateway on port 8090...
start "API Gateway" cmd /k "cd /d %JAR_DIR%\api-gateway && java %HEAP_SIZE% -jar target\api-gateway-1.0.0.jar"
timeout /t 5

REM User Service (Auth)
echo Starting User Service on port 8081...
start "User Service" cmd /k "cd /d %JAR_DIR%\user-service && java %HEAP_SIZE% -jar target\user-service-1.0.0.jar"
timeout /t 5

REM Patient Service
echo Starting Patient Service on port 8082...
start "Patient Service" cmd /k "cd /d %JAR_DIR%\patient-service && java %HEAP_SIZE% -jar target\patient-service-1.0.0.jar"
timeout /t 5

REM Vitals Service
echo Starting Vitals Service on port 8083...
start "Vitals Service" cmd /k "cd /d %JAR_DIR%\vitals-service && java %HEAP_SIZE% -jar target\vitals-service-1.0.0.jar"
timeout /t 5

REM Report Service
echo Starting Report Service on port 8084...
start "Report Service" cmd /k "cd /d %JAR_DIR%\report-service && java %HEAP_SIZE% -jar target\report-service-1.0.0.jar"
timeout /t 5

REM Alert Service
echo Starting Alert Service on port 8085...
start "Alert Service" cmd /k "cd /d %JAR_DIR%\alert-service && java %HEAP_SIZE% -jar target\alert-service-1.0.0.jar"
timeout /t 5

REM Notification Service
echo Starting Notification Service on port 8086...
start "Notification Service" cmd /k "cd /d %JAR_DIR%\notification-service && java %HEAP_SIZE% -jar target\notification-service-1.0.0.jar"
timeout /t 5

echo.
echo ====================================================
echo All services started. Check individual windows for logs.
echo Gateway: http://localhost:8090
echo Eureka: http://localhost:8761
echo ====================================================
echo.

endlocal
