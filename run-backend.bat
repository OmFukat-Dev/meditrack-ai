@echo off
setlocal
cd /d "%~dp0"

set "MAVEN_OPTS=-Xms128m -Xmx768m -XX:MaxMetaspaceSize=384m"
set "PS_CMD=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"

set "BACKEND_MODE=All"
set "BACKEND_START_MESSAGE=Starting backend infrastructure and services..."
if /I "%~1"=="--skip-docker" set "BACKEND_MODE=BackendOnly"
if /I "%1"=="--skip-docker" set "BACKEND_START_MESSAGE=Waiting for Docker infrastructure and starting backend services..."

echo.
echo ========================================
echo   MediTrack AI Backend Services
echo ========================================
echo.

REM Set Maven environment variables
set "MAVEN_HOME=D:\Program Files\Apache\apache-maven-3.9.13"
set "MVEN="

REM Check Option 0: Global Maven in PATH
where mvn >nul 2>nul
if %errorlevel% equ 0 (
  set MVEN=mvn
  goto MAVEN_FOUND
)

REM Check Option 1: Primary Maven Home
if not exist "%MAVEN_HOME%\bin\mvn.cmd" goto TRY_ALT_MAVEN
set MVEN=call "%MAVEN_HOME%\bin\mvn.cmd"
set "PATH=%MAVEN_HOME%\bin;%PATH%"
goto MAVEN_FOUND

:TRY_ALT_MAVEN
echo Trying alternative Maven installation...
set "ALT_MAVEN=C:\Users\fukat\.m2\wrapper\dists\apache-maven-3.8.4-bin\52ccbt68d252mdldqsfsn03jlf\apache-maven-3.8.4"
if not exist "%ALT_MAVEN%\bin\mvn.cmd" goto TRY_WRAPPER
set "MAVEN_HOME=%ALT_MAVEN%"
set MVEN=call "%ALT_MAVEN%\bin\mvn.cmd"
set "PATH=%ALT_MAVEN%\bin;%PATH%"
goto MAVEN_FOUND

:TRY_WRAPPER
echo Trying local Maven Wrapper...
if not exist "%~dp0backend\mvnw.cmd" goto MAVEN_NOT_FOUND
set MVEN=call "%~dp0backend\mvnw.cmd"
goto MAVEN_FOUND

:MAVEN_NOT_FOUND
echo ERROR: Maven not found in any location (including Wrapper).
echo Please install Maven or check backend\mvnw.cmd.
pause
exit /b 1

:MAVEN_FOUND

echo Using Maven command: %MVEN%

REM Check Java version
java -version >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java is not installed or not in PATH
  echo Please install Java 17 or higher
  pause
  exit /b 1
)

echo Building backend services...
echo This may take a few minutes on first run...
echo.

REM Build each service individually for better error handling
echo Building Eureka Server...
cd backend\eureka-server
%MVEN% clean package -DskipTests
if errorlevel 1 (
  echo Eureka Server build failed.
  pause
  exit /b 1
)
cd ..\..

echo Building API Gateway...
cd backend\api-gateway
%MVEN% clean package -DskipTests
if errorlevel 1 (
  echo API Gateway build failed.
  pause
  exit /b 1
)
cd ..\..

echo Building Patient Service...
cd backend\patient-service
%MVEN% clean package -DskipTests
if errorlevel 1 (
  echo Patient Service build failed.
  pause
  exit /b 1
)
cd ..\..

echo Building Vitals Service...
cd backend\vitals-service
%MVEN% clean package -DskipTests
if errorlevel 1 (
  echo Vitals Service build failed.
  pause
  exit /b 1
)
cd ..\..

echo Building Report Service...
cd backend\report-service
%MVEN% clean package -DskipTests
if errorlevel 1 (
  echo Report Service build failed.
  pause
  exit /b 1
)
cd ..\..

echo Building User Service...
cd backend\user-service
%MVEN% clean package -DskipTests
if errorlevel 1 (
  echo User Service build failed.
  pause
  exit /b 1
)
cd ..\..

echo Building AI Prediction Service...
cd backend\ai-prediction
%MVEN% clean package -DskipTests
if errorlevel 1 (
  echo AI Prediction Service build failed.
  pause
  exit /b 1
)
cd ..\..

echo Building Alert Service...
cd backend\alert-service
%MVEN% clean package -DskipTests
if errorlevel 1 (
  echo Alert Service build failed.
  pause
  exit /b 1
)
cd ..\..

echo Building Notification Service...
cd backend\notification-service
%MVEN% clean package -DskipTests
if errorlevel 1 (
  echo Notification Service build failed.
  pause
  exit /b 1
)
cd ..\..

echo.
echo All backend services built successfully!
echo.

echo %BACKEND_START_MESSAGE%
echo.

REM Start services in background windows
echo Starting Eureka Server (Port: 8761)...
start "Eureka Server" cmd /k "cd /d %~dp0backend\eureka-server && java -Xms32m -Xmx96m -XX:MaxMetaspaceSize=80m -jar target\eureka-server-1.0.0.jar"

"%PS_CMD%" -NoProfile -Command "Start-Sleep -Seconds 10" >nul

echo Starting API Gateway (Port: 8090)...
start "API Gateway" cmd /k "cd /d %~dp0backend\api-gateway && java -Dserver.port=8090 -Xms64m -Xmx192m -XX:MaxMetaspaceSize=128m -jar target\api-gateway-1.0.0.jar"

"%PS_CMD%" -NoProfile -Command "Start-Sleep -Seconds 10" >nul

echo Starting Patient Service (Port: 8082)...
start "Patient Service" cmd /k "cd /d %~dp0backend\patient-service && java -Xms32m -Xmx128m -XX:MaxMetaspaceSize=96m -jar target\patient-service-1.0.0.jar"

"%PS_CMD%" -NoProfile -Command "Start-Sleep -Seconds 5" >nul

echo Starting Vitals Service (Port: 8083)...
start "Vitals Service" cmd /k "cd /d %~dp0backend\vitals-service && java -Xms32m -Xmx128m -XX:MaxMetaspaceSize=96m -jar target\vitals-service-1.0.0.jar"

"%PS_CMD%" -NoProfile -Command "Start-Sleep -Seconds 5" >nul

echo Starting Report Service (Port: 8087)...
start "Report Service" cmd /k "cd /d %~dp0backend\report-service && java -Xms32m -Xmx128m -XX:MaxMetaspaceSize=96m -jar target\report-service-1.0.0.jar"

"%PS_CMD%" -NoProfile -Command "Start-Sleep -Seconds 5" >nul

echo Starting User Service (Port: 8081)...
start "User Service" cmd /k "cd /d %~dp0backend\user-service && java -Xms32m -Xmx128m -XX:MaxMetaspaceSize=96m -jar target\user-service-1.0.0.jar"

"%PS_CMD%" -NoProfile -Command "Start-Sleep -Seconds 5" >nul

echo Starting AI Prediction Service (Port: 8084)...
start "AI Prediction Service" cmd /k "cd /d %~dp0backend\ai-prediction && java -Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m -jar target\ai-prediction-1.0.0.jar"

"%PS_CMD%" -NoProfile -Command "Start-Sleep -Seconds 5" >nul

echo Starting Alert Service (Port: 8085)...
start "Alert Service" cmd /k "cd /d %~dp0backend\alert-service && java -Xms32m -Xmx128m -XX:MaxMetaspaceSize=96m -jar target\alert-service-1.0.0.jar"

"%PS_CMD%" -NoProfile -Command "Start-Sleep -Seconds 5" >nul

echo Starting Notification Service (Port: 8086)...
start "Notification Service" cmd /k "cd /d %~dp0backend\notification-service && java -Xms32m -Xmx128m -XX:MaxMetaspaceSize=96m -jar target\notification-service-1.0.0.jar"

echo.
echo ========================================
echo   Backend Services Started
echo ========================================
echo.
echo Services available at:
echo - Eureka Server: http://localhost:8761
echo - API Gateway: http://localhost:8090
echo - User Service: http://localhost:8081
echo - Patient Service: http://localhost:8082
echo - Vitals Service: http://localhost:8083
echo - AI Prediction Service: http://localhost:8084
echo - Alert Service: http://localhost:8085
echo - Notification Service: http://localhost:8086
echo - Report Service: http://localhost:8087
echo.
echo Backend services started in separate windows.
echo Close those windows or run scripts\stop-services.ps1 to stop backend.
endlocal
