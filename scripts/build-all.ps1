# MediTrack AI - Build All Services Script
# This script builds all microservices using Maven

Write-Host "🔨 Building MediTrack AI Services..." -ForegroundColor Green

# Function to build a service
function Build-Service {
    param($serviceName, $servicePath)
    
    Write-Host "`n🏗️ Building $serviceName..." -ForegroundColor Cyan
    
    # Check if service directory exists
    if (-not (Test-Path $servicePath)) {
        Write-Host "⚠️ Directory $servicePath not found. Skipping..." -ForegroundColor Yellow
        return $false
    }
    
    try {
        Set-Location $servicePath
        
        # Run Maven clean install
        $result = & mvn clean install -DskipTests
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ $serviceName built successfully" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ $serviceName build failed" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "❌ Error building $serviceName: $_" -ForegroundColor Red
        return $false
    }
}

# Main execution
Set-Location $PSScriptRoot\..

# Services to build (in dependency order)
$services = @(
    @{name="Eureka Server"; path="backend/eureka-server"},
    @{name="API Gateway"; path="backend/api-gateway"},
    @{name="Patient Service"; path="backend/patient-service"},
    @{name="Vitals Service"; path="backend/vitals-service"},
    @{name="AI Prediction Service"; path="backend/ai-prediction"},
    @{name="Alert Service"; path="backend/alert-service"},
    @{name="Notification Service"; path="backend/notification-service"},
    @{name="Vital Simulator"; path="backend/vital-simulator"},
    @{name="Report Service"; path="backend/report-service"}
)

$successCount = 0
$totalCount = $services.Count

foreach ($service in $services) {
    if (Build-Service -serviceName $service.name -servicePath $service.path) {
        $successCount++
    }
}

# Summary
Write-Host "`n📊 Build Summary:" -ForegroundColor Cyan
Write-Host "✅ Successfully built: $successCount/$totalCount services" -ForegroundColor $(if ($successCount -eq $totalCount) { "Green" } else { "Yellow" })

if ($successCount -eq $totalCount) {
    Write-Host "🎉 All services built successfully!" -ForegroundColor Green
    Write-Host "💡 Tip: Run '.\scripts\start-services.ps1' to start all services" -ForegroundColor Cyan
} else {
    Write-Host "⚠️ Some services failed to build. Check the logs above." -ForegroundColor Yellow
    exit 1
}

# Return to project root
Set-Location $PSScriptRoot\..
