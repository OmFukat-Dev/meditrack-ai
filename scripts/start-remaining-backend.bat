@echo off
setlocal
cd /d "%~dp0.."

where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java was not found. Please install Java 17+ and add it to PATH.
  pause
  exit /b 1
)

set "JAVA_CMD=java"

echo Starting remaining backend services...

if exist "%~dp0..\backend\patient-service\target\patient-service-1.0.0.jar" (
  start "Patient Service" /D "%~dp0..\backend\patient-service" cmd /k "%JAVA_CMD% -jar target\patient-service-1.0.0.jar"
  echo Started Patient Service
) else (
  echo WARNING: Patient Service JAR not found.
)

if exist "%~dp0..\backend\vitals-service\target\vitals-service-1.0.0.jar" (
  start "Vitals Service" /D "%~dp0..\backend\vitals-service" cmd /k "%JAVA_CMD% -jar target\vitals-service-1.0.0.jar"
  echo Started Vitals Service
) else (
  echo WARNING: Vitals Service JAR not found.
)

if exist "%~dp0..\backend\ai-prediction\target\ai-prediction-1.0.0.jar" (
  start "AI Prediction Service" /D "%~dp0..\backend\ai-prediction" cmd /k "%JAVA_CMD% -jar target\ai-prediction-1.0.0.jar"
  echo Started AI Prediction Service
) else (
  echo WARNING: AI Prediction Service JAR not found.
)

if exist "%~dp0..\backend\alert-service\target\alert-service-1.0.0.jar" (
  start "Alert Service" /D "%~dp0..\backend\alert-service" cmd /k "%JAVA_CMD% -jar target\alert-service-1.0.0.jar"
  echo Started Alert Service
) else (
  echo WARNING: Alert Service JAR not found.
)

if exist "%~dp0..\backend\notification-service\target\notification-service-1.0.0.jar" (
  start "Notification Service" /D "%~dp0..\backend\notification-service" cmd /k "%JAVA_CMD% -jar target\notification-service-1.0.0.jar"
  echo Started Notification Service
) else (
  echo WARNING: Notification Service JAR not found.
)

if exist "%~dp0..\backend\report-service\target\report-service-1.0.0.jar" (
  start "Report Service" /D "%~dp0..\backend\report-service" cmd /k "%JAVA_CMD% -jar target\report-service-1.0.0.jar"
  echo Started Report Service
) else (
  echo WARNING: Report Service JAR not found.
)

echo Remaining backend start commands issued.
pause
endlocal
