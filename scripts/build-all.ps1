# MediTrack AI - Build All Services Script
# This script builds all microservices using Maven

Write-Host "Building MediTrack AI Services..." -ForegroundColor Green

function Get-MavenCommand {
    $candidates = @()

    if ($env:MAVEN_HOME) {
        $candidates += (Join-Path $env:MAVEN_HOME 'bin\mvn.cmd')
    }

    $command = Get-Command mvn -ErrorAction SilentlyContinue
    if ($command -and $command.Source) {
        $candidates += $command.Source
    }

    $candidates += 'C:\Program Files\NetBeans-23\netbeans\java\maven\bin\mvn.cmd'
    $candidates += 'C:\Program Files\apache-maven\bin\mvn.cmd'
    $candidates += 'C:\Program Files\Apache\maven\bin\mvn.cmd'

    foreach ($candidate in $candidates | Select-Object -Unique) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    throw 'Maven executable not found. Set MAVEN_HOME or add mvn to PATH.'
}

function Get-MavenRepoLocal {
    param($repoRoot)

    $userProfile = $env:USERPROFILE
    if (-not $userProfile) {
        $userProfile = [Environment]::GetFolderPath('UserProfile')
    }

    $preferredRepo = Join-Path $userProfile '.m2\repository'
    if (Test-Path $preferredRepo) {
        Write-Host "Using Maven local repository: $preferredRepo" -ForegroundColor DarkGray
        return $preferredRepo
    }

    try {
        $null = New-Item -ItemType Directory -Force -Path $preferredRepo -ErrorAction Stop
        Write-Host "Using Maven local repository: $preferredRepo" -ForegroundColor DarkGray
        return $preferredRepo
    }
    catch {
        $fallbackRepo = Join-Path $repoRoot '.m2\repository'
        $null = New-Item -ItemType Directory -Force -Path $fallbackRepo
        Write-Host "Using repo-local Maven repository: $fallbackRepo" -ForegroundColor DarkGray
        return $fallbackRepo
    }
}

function Stop-MeditrackJavaProcesses {
    Write-Host "Checking for already running MediTrack service ports..." -ForegroundColor Yellow

    $ports = @(8761, 8090, 8082, 8083, 8084, 8085, 8086, 8087, 8088)
    $runningPorts = New-Object System.Collections.Generic.HashSet[int]

    foreach ($line in (netstat -ano 2>$null)) {
        if ($line -match '^\s*TCP\s+\S+:(\d+)\s+\S+\s+LISTENING\s+(\d+)\s*$') {
            $port = [int]$matches[1]
            if ($ports -contains $port) {
                $null = $runningPorts.Add($port)
            }
        }
    }

    if ($runningPorts.Count -eq 0) {
        Write-Host "No running MediTrack service processes found." -ForegroundColor DarkGray
        return
    }

    $portsText = ($runningPorts | Sort-Object) -join ', '
    Write-Host "Already running MediTrack service ports: $portsText" -ForegroundColor DarkGray
}

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

function Build-Service {
    param($serviceName, $servicePath, $servicePort, $repoRoot, $mavenCommand)

    Write-Host "Building $serviceName..." -ForegroundColor Cyan

    $absoluteServicePath = Join-Path $repoRoot $servicePath

    if ($servicePort -and (Test-ServicePort -port $servicePort)) {
        Write-Host "$serviceName is already running on port $servicePort. Skipping rebuild and keeping the existing jar." -ForegroundColor DarkYellow
        return $true
    }

    if (-not (Test-Path $absoluteServicePath)) {
        Write-Host "Directory $absoluteServicePath not found. Skipping..." -ForegroundColor Yellow
        return $false
    }

    try {
        Push-Location $absoluteServicePath

        & $mavenCommand "-Dmaven.repo.local=$mavenRepo" clean install "-Dmaven.test.skip=true" 2>&1 | Out-Host

        if ($LASTEXITCODE -eq 0) {
            Write-Host "Service built successfully: $serviceName" -ForegroundColor Green
            return $true
        }

        Write-Host "Service build failed: ${serviceName}" -ForegroundColor Red
        return $false
    }
    catch {
        Write-Host "Error building ${serviceName}: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
    finally {
        Pop-Location | Out-Null
    }
}

$repoRoot = Split-Path $PSScriptRoot -Parent
$mavenRepo = Get-MavenRepoLocal -repoRoot $repoRoot
$mavenCommand = Get-MavenCommand

Stop-MeditrackJavaProcesses

$services = @(
    @{name="Eureka Server"; path="backend/eureka-server"; port=8761},
    @{name="API Gateway"; path="backend/api-gateway"; port=8090},
    @{name="Patient Service"; path="backend/patient-service"; port=8082},
    @{name="Vitals Service"; path="backend/vitals-service"; port=8083},
    @{name="AI Prediction Service"; path="backend/ai-prediction"; port=8084},
    @{name="Alert Service"; path="backend/alert-service"; port=8085},
    @{name="Notification Service"; path="backend/notification-service"; port=8086},
    @{name="Vital Simulator"; path="backend/vital-simulator"; port=8088},
    @{name="Report Service"; path="backend/report-service"; port=8087}
)

$successCount = 0
$totalCount = $services.Count

foreach ($service in $services) {
    if (Build-Service -serviceName $service.name -servicePath $service.path -servicePort $service.port -repoRoot $repoRoot -mavenCommand $mavenCommand) {
        $successCount++
    }
}

Write-Host "`nBuild Summary:" -ForegroundColor Cyan
Write-Host "Successfully built: $successCount/$totalCount services" -ForegroundColor $(if ($successCount -eq $totalCount) { "Green" } else { "Yellow" })

if ($successCount -eq $totalCount) {
    Write-Host "All services built successfully!" -ForegroundColor Green
    Write-Host "Tip: Run '.\scripts\start-services.ps1' to start all services" -ForegroundColor Cyan
}
else {
    Write-Host "Some services failed to build. Check the logs above." -ForegroundColor Yellow
    exit 1
}

Set-Location $repoRoot
