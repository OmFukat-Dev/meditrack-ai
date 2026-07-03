@echo off

echo Starting remaining backend services (absolute paths)...

start "Patient Service" cmd /k "cd /d d:\\Projects\\CascadeProjects\\meditrack-ai\\backend\\patient-service && java -jar target\\patient-service-1.0.0.jar"
echo Started Patient Service

start "Vitals Service" cmd /k "cd /d d:\\Projects\\CascadeProjects\\meditrack-ai\\backend\\vitals-service && java -jar target\\vitals-service-1.0.0.jar"
echo Started Vitals Service

start "AI Prediction Service" cmd /k "cd /d d:\\Projects\\CascadeProjects\\meditrack-ai\\backend\\ai-prediction && java -jar target\\ai-prediction-1.0.0.jar"
echo Started AI Prediction Service

start "Alert Service" cmd /k "cd /d d:\\Projects\\CascadeProjects\\meditrack-ai\\backend\\alert-service && java -jar target\\alert-service-1.0.0.jar"
echo Started Alert Service

start "Notification Service" cmd /k "cd /d d:\\Projects\\CascadeProjects\\meditrack-ai\\backend\\notification-service && java -jar target\\notification-service-1.0.0.jar"
echo Started Notification Service

start "Report Service" cmd /k "cd /d d:\\Projects\\CascadeProjects\\meditrack-ai\\backend\\report-service && java -jar target\\report-service-1.0.0.jar"
echo Started Report Service

echo All start commands issued.
pause
