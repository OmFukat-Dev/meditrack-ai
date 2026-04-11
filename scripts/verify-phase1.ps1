# MediTrack AI - Phase 1 Verification Script
# This script verifies that all Phase 1 components are properly configured

Write-Host "## Phase 1: Foundation Setup Verification ##" -ForegroundColor Cyan

$verificationResults = @()

# Function to check if file exists
function Test-FileExists {
    param($filePath, $description)
    if (Test-Path $filePath) {
        Write-Host "  [PASS] $description" -ForegroundColor Green
        return $true
    } else {
        Write-Host "  [FAIL] $description" -ForegroundColor Red
        return $false
    }
}

# Function to check if directory exists
function Test-DirectoryExists {
    param($dirPath, $description)
    if (Test-Path $dirPath) {
        Write-Host "  [PASS] $description" -ForegroundColor Green
        return $true
    } else {
        Write-Host "  [FAIL] $description" -ForegroundColor Red
        return $false
    }
}

# Function to check Maven project structure
function Test-MavenStructure {
    param($servicePath, $serviceName)
    $hasPom = Test-Path "$servicePath/pom.xml"
    $hasMainJava = Test-Path "$servicePath/src/main/java"
    $hasResources = Test-Path "$servicePath/src/main/resources"
    $hasAppClass = Test-Path "$servicePath/src/main/java/com/meditrack"
    $hasConfig = Test-Path "$servicePath/src/main/resources/application.yml"
    
    if ($hasPom -and $hasMainJava -and $hasResources -and $hasAppClass -and $hasConfig) {
        Write-Host "  [PASS] $serviceName Maven structure" -ForegroundColor Green
        return $true
    } else {
        Write-Host "  [FAIL] $serviceName Maven structure" -ForegroundColor Red
        if (-not $hasPom) { Write-Host "    Missing pom.xml" -ForegroundColor Yellow }
        if (-not $hasMainJava) { Write-Host "    Missing src/main/java" -ForegroundColor Yellow }
        if (-not $hasResources) { Write-Host "    Missing src/main/resources" -ForegroundColor Yellow }
        if (-not $hasAppClass) { Write-Host "    Missing application class" -ForegroundColor Yellow }
        if (-not $hasConfig) { Write-Host "    Missing application.yml" -ForegroundColor Yellow }
        return $false
    }
}

# Function to check Flyway migrations
function Test-FlywayMigrations {
    param($servicePath, $serviceName)
    $migrationPath = "$servicePath/src/main/resources/db/migration"
    if (Test-Path $migrationPath) {
        $migrationFiles = Get-ChildItem $migrationPath -Filter "*.sql" 2>$null
        if ($migrationFiles.Count -gt 0) {
            Write-Host "  [PASS] $serviceName Flyway migrations ($($migrationFiles.Count) files)" -ForegroundColor Green
            return $true
        } else {
            Write-Host "  [FAIL] $serviceName Flyway migrations (no SQL files found)" -ForegroundColor Red
            return $false
        }
    } else {
        Write-Host "  [FAIL] $serviceName Flyway migrations (directory missing)" -ForegroundColor Red
        return $false
    }
}

Write-Host "`n### 1. Project Structure Verification ###" -ForegroundColor Yellow
$verificationResults += Test-FileExists "pom.xml" "Root POM file"
$verificationResults += Test-FileExists "docker-compose.yml" "Docker Compose configuration"
$verificationResults += Test-DirectoryExists "backend" "Backend services directory"
$verificationResults += Test-DirectoryExists "docker" "Docker configurations directory"
$verificationResults += Test-DirectoryExists "scripts" "Scripts directory"

Write-Host "`n### 2. Docker Infrastructure Verification ###" -ForegroundColor Yellow
$verificationResults += Test-FileExists "docker/prometheus/prometheus.yml" "Prometheus configuration"
$verificationResults += Test-FileExists "docker/grafana/provisioning/datasources/prometheus.yml" "Grafana datasource"
$verificationResults += Test-FileExists "docker/grafana/provisioning/dashboards/dashboard.yml" "Grafana dashboard config"

Write-Host "`n### 3. Microservices Structure Verification ###" -ForegroundColor Yellow
$services = @(
    @{path="backend/eureka-server"; name="Eureka Server"},
    @{path="backend/api-gateway"; name="API Gateway"},
    @{path="backend/patient-service"; name="Patient Service"},
    @{path="backend/vitals-service"; name="Vitals Service"},
    @{path="backend/ai-prediction"; name="AI Prediction Service"},
    @{path="backend/alert-service"; name="Alert Service"},
    @{path="backend/notification-service"; name="Notification Service"},
    @{path="backend/report-service"; name="Report Service"},
    @{path="backend/vital-simulator"; name="Vital Simulator"}
)

foreach ($service in $services) {
    $verificationResults += Test-MavenStructure $service.path $service.name
}

Write-Host "`n### 4. Database Schema Verification ###" -ForegroundColor Yellow
$dbServices = @(
    @{path="backend/patient-service"; name="Patient Service"},
    @{path="backend/vitals-service"; name="Vitals Service"},
    @{path="backend/ai-prediction"; name="AI Prediction Service"},
    @{path="backend/alert-service"; name="Alert Service"},
    @{path="backend/notification-service"; name="Notification Service"},
    @{path="backend/report-service"; name="Report Service"}
)

foreach ($service in $dbServices) {
    $verificationResults += Test-FlywayMigrations $service.path $service.name
}

Write-Host "`n### 5. Scripts Verification ###" -ForegroundColor Yellow
$verificationResults += Test-FileExists "scripts/build-all.ps1" "Build all services script"
$verificationResults += Test-FileExists "scripts/start-services.ps1" "Start services script"
$verificationResults += Test-FileExists "scripts/stop-services.ps1" "Stop services script"
$verificationResults += Test-FileExists "scripts/verify-phase1.ps1" "Phase 1 verification script"

Write-Host "`n### 6. Documentation Verification ###" -ForegroundColor Yellow
$verificationResults += Test-FileExists "README.md" "Project README"
$verificationResults += Test-FileExists "MEDITRACK_IMPLEMENTATION_PLAN.md" "Implementation plan"
$verificationResults += Test-FileExists "GIT_SETUP.md" "Git setup instructions"

# Calculate results
$passCount = ($verificationResults | Where-Object { $_ -eq $true }).Count
$failCount = ($verificationResults | Where-Object { $_ -eq $false }).Count
$totalCount = $verificationResults.Count

Write-Host "`n### Verification Results ###" -ForegroundColor Yellow
Write-Host "Total Checks: $totalCount" -ForegroundColor White
Write-Host "Passed: $passCount" -ForegroundColor Green
Write-Host "Failed: $failCount" -ForegroundColor $(if ($failCount -gt 0) { "Red" } else { "Green" })

$successRate = [math]::Round(($passCount / $totalCount) * 100, 2)
Write-Host "Success Rate: $successRate%" -ForegroundColor $(if ($successRate -eq 100) { "Green" } elseif ($successRate -ge 80) { "Yellow" } else { "Red" })

if ($failCount -eq 0) {
    Write-Host "`n[SUCCESS] Phase 1 is complete! Ready to proceed to Phase 2." -ForegroundColor Green
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. Run 'docker-compose up -d' to start infrastructure" -ForegroundColor White
    Write-Host "2. Run '.\scripts\build-all.ps1' to build all services" -ForegroundColor White
    Write-Host "3. Run '.\scripts\start-services.ps1' to start all services" -ForegroundColor White
} else {
    Write-Host "`n[INCOMPLETE] Phase 1 has issues that need to be resolved." -ForegroundColor Red
    Write-Host "Please fix the failed items above before proceeding to Phase 2." -ForegroundColor Yellow
}

Write-Host "`nPhase 1 verification completed." -ForegroundColor Cyan
