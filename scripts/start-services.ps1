# MediTrack AI - Services Startup Script
# This script starts all infrastructure services and microservices

Write-Host "🚀 Starting MediTrack AI Services..." -ForegroundColor Green

# Function to check if a service is running
function Test-ServicePort {
    param($port)
    try {
        $connection = New-Object System.Net.Sockets.TcpClient
        $connection.Connect("localhost", $port)
        $connection.Close()
        return $true
    }
    catch {
        return $false
    }
}

# Function to wait for service
function Wait-ForService {
    param($serviceName, $port, $timeout = 60)
    Write-Host "⏳ Waiting for $serviceName (port $port)..." -ForegroundColor Yellow
    
    $elapsed = 0
    while (-not (Test-ServicePort -port $port) -and $elapsed -lt $timeout) {
        Start-Sleep 2
        $elapsed += 2
        Write-Host "." -NoNewline -ForegroundColor Yellow
    }
    
    if (Test-ServicePort -port $port) {
        Write-Host " ✅ $serviceName is ready!" -ForegroundColor Green
        return $true
    } else {
        Write-Host " ❌ $serviceName failed to start within $timeout seconds" -ForegroundColor Red
        return $false
    }
}

# Step 1: Start Docker Infrastructure Services
Write-Host "`n📦 Starting Docker Infrastructure Services..." -ForegroundColor Cyan
Set-Location $PSScriptRoot\..

# Check if Docker is running
try {
    docker version | Out-Null
    Write-Host "✅ Docker is running" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Start infrastructure services
Write-Host "Starting MySQL, Redis, Kafka, Prometheus, Grafana, Zipkin..." -ForegroundColor Cyan
docker-compose up -d mysql redis zookeeper kafka prometheus grafana zipkin

# Wait for infrastructure services
Write-Host "`n⏳ Waiting for infrastructure services to be healthy..." -ForegroundColor Yellow
Wait-ForService -serviceName "MySQL" -port 3306 -timeout 60
Wait-ForService -serviceName "Redis" -port 6379 -timeout 30
Wait-ForService -serviceName "Kafka" -port 9092 -timeout 90
Wait-ForService -serviceName "Prometheus" -port 9090 -timeout 30
Wait-ForService -serviceName "Grafana" -port 3000 -timeout 30
Wait-ForService -serviceName "Zipkin" -port 9411 -timeout 30

# Step 2: Start Spring Boot Services
Write-Host "`n🏗️ Starting Spring Boot Microservices..." -ForegroundColor Cyan

# Service startup order and ports
$services = @(
    @{name="Eureka Server"; path="backend/eureka-server"; port=8761},
    @{name="API Gateway"; path="backend/api-gateway"; port=8080},
    @{name="Patient Service"; path="backend/patient-service"; port=8081},
    @{name="Vitals Service"; path="backend/vitals-service"; port=8082},
    @{name="AI Prediction Service"; path="backend/ai-prediction"; port=8083},
    @{name="Alert Service"; path="backend/alert-service"; port=8084},
    @{name="Notification Service"; path="backend/notification-service"; port=8085},
    @{name="Vital Simulator"; path="backend/vital-simulator"; port=8086},
    @{name="Report Service"; path="backend/report-service"; port=8087}
)

foreach ($service in $services) {
    Write-Host "`n🚀 Starting $($service.name)..." -ForegroundColor Cyan
    
    # Check if service directory exists
    if (-not (Test-Path $service.path)) {
        Write-Host "⚠️ Directory $($service.path) not found. Skipping..." -ForegroundColor Yellow
        continue
    }
    
    # Start the service in background
    $serviceProcess = Start-Process -FilePath "java" -ArgumentList "-jar", "target/$($service.path.Split('/')[-1])-1.0.0.jar" -WorkingDirectory $service.path -PassThru -WindowStyle Hidden
    
    # Wait a bit for service to start
    Start-Sleep 10
    
    # Check if service is responding
    if (Wait-ForService -serviceName $service.name -port $service.port -timeout 60) {
        Write-Host "✅ $($service.name) started successfully" -ForegroundColor Green
    } else {
        Write-Host "❌ $($service.name) failed to start" -ForegroundColor Red
    }
}

# Step 3: Display Service URLs
Write-Host "`n🌐 Service URLs:" -ForegroundColor Cyan
Write-Host "📊 Grafana Dashboard: http://localhost:3000 (admin/admin123)" -ForegroundColor White
Write-Host "📈 Prometheus Metrics: http://localhost:9090" -ForegroundColor White
Write-Host "🔍 Zipkin Tracing: http://localhost:9411" -ForegroundColor White
Write-Host "📡 Kafka UI: http://localhost:8081" -ForegroundColor White
Write-Host "🏥 Eureka Server: http://localhost:8761 (admin/admin123)" -ForegroundColor White
Write-Host "🚪 API Gateway: http://localhost:8080" -ForegroundColor White

Write-Host "`n🎉 MediTrack AI services startup complete!" -ForegroundColor Green
Write-Host "💡 Tip: Check individual service logs in their respective directories" -ForegroundColor Yellow
