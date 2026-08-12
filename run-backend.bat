@echo off
setlocal
cd /d "%~dp0"

set "ROOT_DIR=%~dp0"
set "MAVEN_WRAPPER=%ROOT_DIR%backend\mvnw.cmd"
set "MVN_CMD="

if exist "%MAVEN_WRAPPER%" (
  set "MVN_CMD=%MAVEN_WRAPPER%"
) else (
  where mvn >nul 2>nul
  if %errorlevel% equ 0 (
    set "MVN_CMD=mvn"
  )
)

if "%MVN_CMD%"=="" (
  echo ERROR: Maven was not found. Please install Maven or ensure backend\mvnw.cmd exists.
  pause
  exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java was not found. Please install Java 17+ and add it to PATH.
  pause
  exit /b 1
)

set "JAVA_CMD=java"

echo.
echo ========================================
echo   MediTrack AI Backend Startup
echo ========================================
echo.

echo Using Maven command: %MVN_CMD%
echo Using Java command: %JAVA_CMD%
echo.

echo Building backend services...
echo Building backend services...
call "%MVN_CMD%" -f "%ROOT_DIR%pom.xml" -pl backend/eureka-server -am -DskipTests clean package
if errorlevel 1 (
  echo ERROR: Eureka Server build failed.
  pause
  exit /b 1
)
echo DEBUG: after eureka build
call "%MVN_CMD%" -f "%ROOT_DIR%pom.xml" -pl backend/api-gateway -am -DskipTests clean package
if errorlevel 1 (
  echo ERROR: API Gateway build failed.
  pause
  exit /b 1
)
echo DEBUG: after gateway build
call "%MVN_CMD%" -f "%ROOT_DIR%pom.xml" -pl backend/patient-service -am -DskipTests clean package
if errorlevel 1 (
  echo ERROR: Patient Service build failed.
  pause
  exit /b 1
)
echo DEBUG: after patient build
call "%MVN_CMD%" -f "%ROOT_DIR%pom.xml" -pl backend/vitals-service -am -DskipTests clean package
if errorlevel 1 (
  echo ERROR: Vitals Service build failed.
  pause
  exit /b 1
)
call "%MVN_CMD%" -f "%ROOT_DIR%pom.xml" -pl backend/report-service -am -DskipTests clean package
if errorlevel 1 (
  echo ERROR: Report Service build failed.
  pause
  exit /b 1
)
call "%MVN_CMD%" -f "%ROOT_DIR%pom.xml" -pl backend/user-service -am -DskipTests clean package
if errorlevel 1 (
  echo ERROR: User Service build failed.
  pause
  exit /b 1
)
call "%MVN_CMD%" -f "%ROOT_DIR%pom.xml" -pl backend/ai-prediction -am -DskipTests clean package
if errorlevel 1 (
  echo ERROR: AI Prediction Service build failed.
  pause
  exit /b 1
)
call "%MVN_CMD%" -f "%ROOT_DIR%pom.xml" -pl backend/alert-service -am -DskipTests clean package
if errorlevel 1 (
  echo ERROR: Alert Service build failed.
  pause
  exit /b 1
)
call "%MVN_CMD%" -f "%ROOT_DIR%pom.xml" -pl backend/notification-service -am -DskipTests clean package
if errorlevel 1 (
  echo ERROR: Notification Service build failed.
  pause
  exit /b 1
)
call "%MVN_CMD%" -f "%ROOT_DIR%pom.xml" -pl backend/vital-simulator -am -DskipTests clean package
if errorlevel 1 (
  echo ERROR: Vital Simulator build failed.
  pause
  exit /b 1
)

echo.
echo All backend artifacts built successfully.
echo.
echo DEBUG: reached startup section

echo Starting Eureka Server (Port: 8761)...
start "Eureka Server" /D "%ROOT_DIR%backend\eureka-server" cmd /k "%JAVA_CMD% -jar target\eureka-server-1.0.0.jar"

echo Starting API Gateway (Port: 8090)...
start "API Gateway" /D "%ROOT_DIR%backend\api-gateway" cmd /k "%JAVA_CMD% -Dserver.port=8090 -jar target\api-gateway-1.0.0.jar"

echo Starting Patient Service (Port: 8082)...
start "Patient Service" /D "%ROOT_DIR%backend\patient-service" cmd /k "%JAVA_CMD% -jar target\patient-service-1.0.0.jar"

echo Starting Vitals Service (Port: 8083)...
start "Vitals Service" /D "%ROOT_DIR%backend\vitals-service" cmd /k "%JAVA_CMD% -jar target\vitals-service-1.0.0.jar"

echo Starting Report Service (Port: 8087)...
start "Report Service" /D "%ROOT_DIR%backend\report-service" cmd /k "%JAVA_CMD% -jar target\report-service-1.0.0.jar"

echo Starting User Service (Port: 8081)...
start "User Service" /D "%ROOT_DIR%backend\user-service" cmd /k "%JAVA_CMD% -jar target\user-service-1.0.0.jar"

echo Starting AI Prediction Service (Port: 8084)...
start "AI Prediction Service" /D "%ROOT_DIR%backend\ai-prediction" cmd /k "%JAVA_CMD% -jar target\ai-prediction-1.0.0.jar"

echo Starting Alert Service (Port: 8085)...
start "Alert Service" /D "%ROOT_DIR%backend\alert-service" cmd /k "%JAVA_CMD% -jar target\alert-service-1.0.0.jar"

echo Starting Notification Service (Port: 8086)...
start "Notification Service" /D "%ROOT_DIR%backend\notification-service" cmd /k "%JAVA_CMD% -jar target\notification-service-1.0.0.jar"

echo Starting Vital Simulator (Port: 8088)...
start "Vital Simulator" /D "%ROOT_DIR%backend\vital-simulator" cmd /k "%JAVA_CMD% -jar target\vital-simulator-1.0.0.jar"

echo.
echo Backend services started in separate windows.
echo API Gateway should be available at http://localhost:8090
echo.
endlocal
