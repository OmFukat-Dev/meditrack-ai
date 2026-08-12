#!/usr/bin/env pwsh
# MediTrack AI - Service Startup Script
# This script starts all backend services with proper sequencing

$ErrorActionPreference = "Continue"
$root = 'd:\Projects\CascadeProjects\meditrack-ai'

function Wait-Port ($Name, $Port, $TimeoutSeconds = 60) {
    Write-Host "Waiting for $Name to be ready on port $Port..." -ForegroundColor Yellow
    $start = Get-Date
    while ($true) {
        try {
            $tcpClient = New-Object System.Net.Sockets.TcpClient
            $tcpClient.Connect('127.0.0.1', $Port)
            $tcpClient.Close()
            Write-Host "$Name is ready!" -ForegroundColor Green
            return $true
        } catch {
            if (((Get-Date) - $start).TotalSeconds -gt $TimeoutSeconds) {
                Write-Host "WARNING: Timeout waiting for $Name on port $Port" -ForegroundColor Red
                return $false
            }
            Start-Sleep -Seconds 2
        }
    }
}

$services = @(
    @{Name='eureka-server'; Port=8761; Args=@()},
    @{Name='api-gateway'; Port=8090; Args=@('-Dserver.port=8090')},
    @{Name='patient-service'; Port=8082; Args=@()},
    @{Name='vitals-service'; Port=8083; Args=@()},
    @{Name='notification-service'; Port=8086; Args=@()},
    @{Name='user-service'; Port=8081; Args=@()},
    @{Name='report-service'; Port=8087; Args=@()},
    @{Name='ai-prediction'; Port=8084; Args=@()},
    @{Name='alert-service'; Port=8085; Args=@()}
)

Write-Host "Starting MediTrack AI Backend Services..." -ForegroundColor Green

# 1. Start Eureka Server
$eureka = $services | Where-Object { $_.Name -eq 'eureka-server' }
$jarPath = Join-Path $root ('backend/' + $eureka.Name + '/target/' + $eureka.Name + '-1.0.0.jar')
if (Test-Path $jarPath) {
    $javaArgs = @('-Xms32m', '-Xmx192m', '-jar') + $eureka.Args + @($jarPath)
    Write-Host "Starting eureka-server on port 8761..." -ForegroundColor Cyan
    Start-Process -FilePath 'java' -ArgumentList $javaArgs -WindowStyle Hidden
    Wait-Port 'eureka-server' 8761 60
} else {
    Write-Host "ERROR: eureka-server jar not found!" -ForegroundColor Red
    exit 1
}

# 2. Start Patient Service (core database migration dependency)
$patient = $services | Where-Object { $_.Name -eq 'patient-service' }
$jarPath = Join-Path $root ('backend/' + $patient.Name + '/target/' + $patient.Name + '-1.0.0.jar')
if (Test-Path $jarPath) {
    $javaArgs = @('-Xms32m', '-Xmx192m', '-jar') + $patient.Args + @($jarPath)
    Write-Host "Starting patient-service on port 8082..." -ForegroundColor Cyan
    Start-Process -FilePath 'java' -ArgumentList $javaArgs -WindowStyle Hidden
    Wait-Port 'patient-service' 8082 90
} else {
    Write-Host "ERROR: patient-service jar not found!" -ForegroundColor Red
    exit 1
}

# 3. Start remaining services
foreach ($svc in $services) {
    if ($svc.Name -eq 'eureka-server' -or $svc.Name -eq 'patient-service') {
        continue
    }
    
    $jarPath = Join-Path $root ('backend/' + $svc.Name + '/target/' + $svc.Name + '-1.0.0.jar')
    if (-not (Test-Path $jarPath)) {
        Write-Host ("SKIP: " + $svc.Name + " - jar not found") -ForegroundColor Yellow
        continue
    }
    
    $javaArgs = @('-Xms32m', '-Xmx192m', '-jar') + $svc.Args + @($jarPath)
    Write-Host ("Starting " + $svc.Name + " on port " + $svc.Port + "...") -ForegroundColor Cyan
    
    try {
        Start-Process -FilePath 'java' -ArgumentList $javaArgs -WindowStyle Hidden -ErrorAction Stop
        Start-Sleep -Seconds 3
    } catch {
        Write-Host ("ERROR starting " + $svc.Name + ": " + $_.Exception.Message) -ForegroundColor Red
    }
}

Write-Host "All services started. Waiting for initialization..." -ForegroundColor Green
Start-Sleep -Seconds 15

Write-Host "Checking service ports..." -ForegroundColor Cyan
foreach ($svc in $services) {
    try {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $tcpClient.Connect('127.0.0.1', $svc.Port)
        $tcpClient.Close()
        Write-Host ($svc.Name + ": READY on port " + $svc.Port) -ForegroundColor Green
    } catch {
        Write-Host ($svc.Name + ": NOT READY (port " + $svc.Port + ")") -ForegroundColor Yellow
    }
}

Write-Host "Backend services startup complete!" -ForegroundColor Green
Write-Host "Keeping script active to prevent microservices from exiting. Press Ctrl+C to terminate." -ForegroundColor Yellow
while ($true) {
    Start-Sleep -Seconds 10
}
