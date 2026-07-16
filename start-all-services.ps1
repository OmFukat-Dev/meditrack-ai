#!/usr/bin/env pwsh
# MediTrack AI - Service Startup Script
# This script starts all backend services with proper sequencing

$ErrorActionPreference = "Continue"
$root = 'd:\Projects\CascadeProjects\meditrack-ai'

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

foreach ($svc in $services) {
    $jarPath = Join-Path $root ('backend/' + $svc.Name + '/target/' + $svc.Name + '-1.0.0.jar')
    
    if (-not (Test-Path $jarPath)) {
        Write-Host ("SKIP: " + $svc.Name + " - jar not found") -ForegroundColor Yellow
        continue
    }
    
    $javaArgs = @('-Xms128m', '-Xmx512m', '-jar') + $svc.Args + @($jarPath)
    Write-Host ("Starting " + $svc.Name + " on port " + $svc.Port + "...") -ForegroundColor Cyan
    
    try {
        Start-Process -FilePath 'java' -ArgumentList $javaArgs -WindowStyle Hidden -ErrorAction Stop
        Start-Sleep -Seconds 5
    } catch {
        Write-Host ("ERROR starting " + $svc.Name + ": " + $_.Exception.Message) -ForegroundColor Red
    }
}

Write-Host "All services started. Waiting for initialization..." -ForegroundColor Green
Start-Sleep -Seconds 20

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
