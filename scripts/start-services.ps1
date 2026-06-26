param(
    [ValidateSet('All', 'DockerOnly', 'BackendOnly')]
    [string]$Mode = 'All'
)

# MediTrack AI - Services Startup Script
# This script can start the Docker infrastructure, the Java services, or both.

Write-Host "Starting MediTrack AI Services..." -ForegroundColor Green

$infrastructureServices = @(
    @{ Name = "MySQL"; Port = 3306; Timeout = 60 },
    @{ Name = "Redis"; Port = 6379; Timeout = 30 },
    @{ Name = "Kafka"; Port = 9092; Timeout = 90 },
    @{ Name = "Kafka UI"; Port = 8095; Timeout = 30 },
    @{ Name = "Loki"; Port = 3100; Timeout = 30 },
    @{ Name = "Prometheus"; Port = 9090; Timeout = 30 },
    @{ Name = "Grafana"; Port = 3000; Timeout = 30 },
    @{ Name = "Zipkin"; Port = 9411; Timeout = 30 }
)

$backendServices = @(
    @{ Name = "Eureka Server"; Path = "backend/eureka-server"; Port = 8761; JavaArgs = @("-Xms64m", "-Xmx256m", "-XX:+UseG1GC") },
    @{ Name = "API Gateway"; Path = "backend/api-gateway"; Port = 8090; JavaArgs = @("-Xms64m", "-Xmx256m", "-XX:+UseG1GC") },
    @{ Name = "Patient Service"; Path = "backend/patient-service"; Port = 8082; JavaArgs = @("-Xms64m", "-Xmx256m", "-XX:+UseG1GC") },
    @{ Name = "Vitals Service"; Path = "backend/vitals-service"; Port = 8083; JavaArgs = @("-Xms64m", "-Xmx256m", "-XX:+UseG1GC") },
    @{ Name = "AI Prediction Service"; Path = "backend/ai-prediction"; Port = 8084; JavaArgs = @("-Xms64m", "-Xmx256m", "-XX:+UseG1GC") },
    @{ Name = "Alert Service"; Path = "backend/alert-service"; Port = 8085; JavaArgs = @("-Xms64m", "-Xmx256m", "-XX:+UseG1GC") },
    @{ Name = "Notification Service"; Path = "backend/notification-service"; Port = 8086; JavaArgs = @("-Xms64m", "-Xmx256m", "-XX:+UseG1GC") },
    @{ Name = "Report Service"; Path = "backend/report-service"; Port = 8087; JavaArgs = @("-Xms128m", "-Xmx512m", "-XX:+UseG1GC") },
    @{ Name = "Vital Simulator"; Path = "backend/vital-simulator"; Port = 8088; JavaArgs = @("-Xms128m", "-Xmx512m", "-XX:+UseG1GC") }
)

function Get-DockerExecutable {
    $dockerCommand = Get-Command docker -ErrorAction SilentlyContinue
    if ($dockerCommand) {
        if ($dockerCommand.Source) {
            return $dockerCommand.Source
        }

        if ($dockerCommand.Definition) {
            return $dockerCommand.Definition
        }
    }

    return $null
}

function Test-DockerDesktopRunning {
    try {
        return @(Get-Process -ErrorAction SilentlyContinue | Where-Object {
            $_.ProcessName -in @('Docker Desktop', 'com.docker.backend')
        }).Count -gt 0
    }
    catch {
        return $false
    }
}

function Invoke-DockerCommand {
    param(
        [string]$Arguments,
        [int]$TimeoutSeconds = 15
    )

    $dockerExe = Get-DockerExecutable
    if (-not $dockerExe) {
        return $null
    }

    try {
        $processInfo = New-Object System.Diagnostics.ProcessStartInfo
        $processInfo.FileName = $dockerExe
        $processInfo.Arguments = $Arguments
        $processInfo.RedirectStandardOutput = $true
        $processInfo.RedirectStandardError = $true
        $processInfo.UseShellExecute = $false
        $processInfo.CreateNoWindow = $true

        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $processInfo
        $null = $process.Start()

        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            try {
                $process.Kill()
            }
            catch {
                # Ignore kill failures and treat the command as unavailable for this probe.
            }

            return $null
        }

        return [PSCustomObject]@{
            ExitCode = $process.ExitCode
            StdOut   = $process.StandardOutput.ReadToEnd()
            StdErr   = $process.StandardError.ReadToEnd()
        }
    }
    catch {
        return $null
    }
}

function Start-DockerDesktopCommand {
    param(
        [string[]]$Arguments
    )

    $dockerExe = Get-DockerExecutable
    if (-not $dockerExe) {
        return $false
    }

    try {
        Start-Process -FilePath $dockerExe -ArgumentList $Arguments -WindowStyle Hidden -ErrorAction Stop | Out-Null
        return $true
    }
    catch {
        return $false
    }
}

function Get-WslExecutable {
    $wslPath = Join-Path $env:SystemRoot 'System32\wsl.exe'
    if (Test-Path $wslPath) {
        return $wslPath
    }

    $wslCommand = Get-Command wsl -ErrorAction SilentlyContinue
    if ($wslCommand) {
        if ($wslCommand.Source) {
            return $wslCommand.Source
        }

        if ($wslCommand.Definition) {
            return $wslCommand.Definition
        }
    }

    return $null
}

function Invoke-WslShutdown {
    $wslExe = Get-WslExecutable
    if (-not $wslExe) {
        return $false
    }

    try {
        $processInfo = New-Object System.Diagnostics.ProcessStartInfo
        $processInfo.FileName = $wslExe
        $processInfo.Arguments = '--shutdown'
        $processInfo.RedirectStandardOutput = $true
        $processInfo.RedirectStandardError = $true
        $processInfo.UseShellExecute = $false
        $processInfo.CreateNoWindow = $true

        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $processInfo
        $null = $process.Start()

        if (-not $process.WaitForExit(15000)) {
            try {
                $process.Kill()
            }
            catch {
                # Ignore kill failures and continue with the Docker recovery flow.
            }
        }

        return $true
    }
    catch {
        return $false
    }
}

function Get-DockerDesktopStatus {
    $result = Invoke-DockerCommand -Arguments 'desktop status --format json' -TimeoutSeconds 5

    if (-not $result -or $result.ExitCode -ne 0 -or -not $result.StdOut) {
        return $null
    }

    try {
        return $result.StdOut | ConvertFrom-Json
    }
    catch {
        return $null
    }
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

function Wait-ForService {
    param($serviceName, $port, $timeout = 60)
    Write-Host "Waiting for $serviceName (port $port)..." -ForegroundColor Yellow

    $elapsed = 0
    $isReady = Test-ServicePort -port $port

    while (-not $isReady -and $elapsed -lt $timeout) {
        Start-Sleep 2
        $elapsed += 2
        Write-Host "." -NoNewline -ForegroundColor Yellow
        $isReady = Test-ServicePort -port $port
    }

    if ($isReady) {
        Write-Host " $serviceName is ready!" -ForegroundColor Green
        return $true
    }

    Write-Host " $serviceName failed to start within $timeout seconds" -ForegroundColor Red
    return $false
}

function Test-DockerDaemonRunning {
    param(
        [int]$TimeoutSeconds = 4
    )

    $result = Invoke-DockerCommand -Arguments 'info --format "{{.ServerVersion}}"' -TimeoutSeconds $TimeoutSeconds
    return ($result -ne $null -and $result.ExitCode -eq 0)
}

function Start-DockerDesktop {
    $desktopStatus = Get-DockerDesktopStatus
    if ($desktopStatus -and $desktopStatus.Status -eq 'running') {
        Write-Host "Docker Desktop is already running." -ForegroundColor Yellow
        return $true
    }

    if (Start-DockerDesktopCommand -Arguments @('desktop', 'start', '--detach', '--timeout', '30')) {
        Write-Host "Requested Docker Desktop start..." -ForegroundColor Yellow
        return $true
    }

    $candidatePaths = @()

    if ($env:ProgramFiles) {
        $candidatePaths += (Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe')
    }

    if (${env:ProgramFiles(x86)}) {
        $candidatePaths += (Join-Path ${env:ProgramFiles(x86)} 'Docker\Docker\Docker Desktop.exe')
    }

    if ($env:LOCALAPPDATA) {
        $candidatePaths += (Join-Path $env:LOCALAPPDATA 'Docker\Docker Desktop.exe')
    }

    $candidates = $candidatePaths | Where-Object { $_ -and (Test-Path $_) }

    foreach ($candidate in $candidates) {
        try {
            Write-Host "Launching Docker Desktop from $candidate..." -ForegroundColor Yellow
            Start-Process -FilePath $candidate | Out-Null
            return $true
        }
        catch {
            Write-Host "Failed to launch Docker Desktop from $candidate" -ForegroundColor Yellow
        }
    }

    return $false
}

function Wait-ForDockerDaemon {
    param(
        [int]$timeout = 120,
        [int]$probeTimeoutSeconds = 4
    )

    if (Test-DockerDaemonRunning -TimeoutSeconds $probeTimeoutSeconds) {
        return $true
    }

    Write-Host "Waiting for Docker daemon to become available..." -ForegroundColor Yellow

    $elapsed = 0
    $isReady = $false

    while (-not $isReady -and $elapsed -lt $timeout) {
        Start-Sleep 2
        $elapsed += 2
        Write-Host "." -NoNewline -ForegroundColor Yellow
        $isReady = Test-DockerDaemonRunning -TimeoutSeconds $probeTimeoutSeconds
    }

    if ($isReady) {
        Write-Host " Docker daemon is running" -ForegroundColor Green
        return $true
    }

    Write-Host " Docker daemon did not become ready within $timeout seconds." -ForegroundColor Red
    return $false
}

function Ensure-DockerDaemonRunning {
    param($timeout = 120)

    if (Test-DockerDaemonRunning) {
        return $true
    }

    $desktopStatus = Get-DockerDesktopStatus
    if ($desktopStatus -and $desktopStatus.Status -eq 'running') {
        Write-Host "Docker Desktop is running, but the engine is not responding yet." -ForegroundColor Yellow
        Write-Host "Waiting 15 seconds before requesting a Docker Desktop restart..." -ForegroundColor Yellow

        if (Wait-ForDockerDaemon -timeout 15) {
            return $true
        }

        Write-Host "Resetting WSL to clear the Docker Desktop integration..." -ForegroundColor Yellow
        [void](Invoke-WslShutdown)
        Start-Sleep 5

        Write-Host "Restarting Docker Desktop to recover the engine..." -ForegroundColor Yellow

        if (-not (Start-DockerDesktopCommand -Arguments @('desktop', 'restart', '--detach', '--timeout', '30'))) {
            Write-Host "Unable to request a Docker Desktop restart. Try restarting Docker Desktop manually." -ForegroundColor Red
            return $false
        }

        return (Wait-ForDockerDaemon -timeout $timeout)
    }

    Write-Host "Docker daemon is not running. Attempting to start Docker Desktop..." -ForegroundColor Yellow

    if (-not (Start-DockerDesktop)) {
        Write-Host "Docker Desktop executable was not found. Start Docker Desktop manually and rerun the launcher." -ForegroundColor Red
        return $false
    }

    return (Wait-ForDockerDaemon -timeout $timeout)
}

function Wait-ForInfrastructureServices {
    param(
        [string[]]$ServiceNames = @('MySQL', 'Redis', 'Kafka')
    )

    $allReady = $true
    $servicesToWaitFor = $infrastructureServices | Where-Object { $ServiceNames -contains $_.Name }

    foreach ($service in $servicesToWaitFor) {
        if (-not (Wait-ForService -serviceName $service.Name -port $service.Port -timeout $service.Timeout)) {
            $allReady = $false
        }
    }

    return $allReady
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
        Write-Host "No running MediTrack service ports found." -ForegroundColor DarkGray
        return
    }

    $portsText = ($runningPorts | Sort-Object) -join ', '
    Write-Host "Already running MediTrack service ports: $portsText" -ForegroundColor DarkGray
}

function Get-MySqlClientPath {
    $candidateSources = @()

    $command = Get-Command mysql -ErrorAction SilentlyContinue
    if ($command -and $command.Source) {
        $candidateSources += $command.Source
    }

    $candidateSources += 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
    $candidateSources += 'C:\Program Files\MySQL\MySQL Server 9.0\bin\mysql.exe'

    foreach ($candidate in $candidateSources | Select-Object -Unique) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    return $null
}

function Initialize-MySqlAccounts {
    param(
        [int]$Timeout = 120
    )

    $bootstrapSql = @'
CREATE DATABASE IF NOT EXISTS meditrack_db;
CREATE DATABASE IF NOT EXISTS meditrack_ai_prediction;
CREATE DATABASE IF NOT EXISTS meditrack_alert_service;
DROP USER IF EXISTS 'meditrack_user'@'%';
DROP USER IF EXISTS 'meditrack_user'@'127.0.0.1';
DROP USER IF EXISTS 'meditrack_user'@'localhost';
CREATE USER 'meditrack_user'@'%' IDENTIFIED BY 'meditrack_pass';
CREATE USER 'meditrack_user'@'127.0.0.1' IDENTIFIED BY 'meditrack_pass';
CREATE USER 'meditrack_user'@'localhost' IDENTIFIED BY 'meditrack_pass';
GRANT ALL PRIVILEGES ON meditrack_db.* TO 'meditrack_user'@'%';
GRANT ALL PRIVILEGES ON meditrack_ai_prediction.* TO 'meditrack_user'@'%';
GRANT ALL PRIVILEGES ON meditrack_alert_service.* TO 'meditrack_user'@'%';
GRANT ALL PRIVILEGES ON meditrack_db.* TO 'meditrack_user'@'127.0.0.1';
GRANT ALL PRIVILEGES ON meditrack_ai_prediction.* TO 'meditrack_user'@'127.0.0.1';
GRANT ALL PRIVILEGES ON meditrack_alert_service.* TO 'meditrack_user'@'127.0.0.1';
GRANT ALL PRIVILEGES ON meditrack_db.* TO 'meditrack_user'@'localhost';
GRANT ALL PRIVILEGES ON meditrack_ai_prediction.* TO 'meditrack_user'@'localhost';
GRANT ALL PRIVILEGES ON meditrack_alert_service.* TO 'meditrack_user'@'localhost';
FLUSH PRIVILEGES;
'@

    # Try executing inside container first (avoids dependency on host mysql client)
    $dockerExe = Get-DockerExecutable
    if ($dockerExe) {
        Write-Host "Docker found. Attempting database initialization inside container 'meditrack-mysql'..." -ForegroundColor Yellow
        $singleLineSql = $bootstrapSql -replace "\r?\n", " " -replace '"', '\"'
        
        $elapsed = 0
        while ($elapsed -lt $Timeout) {
            Write-Host "Synchronizing MySQL credentials inside container (via Docker)..." -ForegroundColor Yellow
            $result = Invoke-DockerCommand -Arguments "exec -i meditrack-mysql mysql -uroot -proot123 -e `"$singleLineSql`"" -TimeoutSeconds 10
            
            if ($result -and $result.ExitCode -eq 0) {
                Write-Host "MySQL credentials synchronized inside container successfully." -ForegroundColor Green
                return $true
            }
            
            Start-Sleep 3
            $elapsed += 3
        }
    }

    # Fallback to local host client if docker command was unavailable or failed
    Write-Host "Docker-based initialization failed or was unavailable. Falling back to host MySQL client..." -ForegroundColor Yellow
    $mysqlClient = Get-MySqlClientPath
    if (-not $mysqlClient) {
        Write-Host "MySQL client executable was not found on host." -ForegroundColor Red
        return $false
    }

    $elapsed = 0
    while ($elapsed -lt $Timeout) {
        Write-Host "Synchronizing MySQL credentials on host..." -ForegroundColor Yellow

        try {
            & $mysqlClient --connect-timeout=3 -uroot -proot123 -h 127.0.0.1 -P 3306 -e $bootstrapSql 2>$null | Out-Null
        }
        catch {
            # Retry until the container is fully ready.
        }

        if ($LASTEXITCODE -eq 0) {
            Write-Host "MySQL credentials synchronized on host." -ForegroundColor Green
            return $true
        }

        Start-Sleep 3
        $elapsed += 3
    }

    Write-Host "Unable to synchronize MySQL credentials within $Timeout seconds." -ForegroundColor Red
    return $false
}

function Start-DockerInfrastructure {
    Set-Location $PSScriptRoot\..

    if (-not (Ensure-DockerDaemonRunning)) {
        return $false
    }

    Write-Host "Starting MySQL, Redis, Kafka, Kafka UI, Loki, Promtail, Prometheus, Grafana, Zipkin..." -ForegroundColor Cyan
    $composeCommand = if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
        'docker-compose'
    }
    elseif (Get-Command docker -ErrorAction SilentlyContinue) {
        'docker'
    }
    else {
        $null
    }

    if (-not $composeCommand) {
        Write-Host "Neither docker-compose nor docker was found in PATH." -ForegroundColor Red
        return $false
    }

    if ($composeCommand -eq 'docker-compose') {
        docker-compose up -d mysql redis zookeeper kafka kafka-init kafka-ui loki promtail prometheus grafana zipkin
    }
    else {
        docker compose up -d mysql redis zookeeper kafka kafka-init kafka-ui loki promtail prometheus grafana zipkin
    }

    if ($LASTEXITCODE -ne 0) {
        Write-Host "docker-compose failed to start the infrastructure stack." -ForegroundColor Red
        return $false
    }

    Write-Host "`nWaiting for core infrastructure services to be ready..." -ForegroundColor Yellow
    if (-not (Wait-ForInfrastructureServices -ServiceNames @('MySQL', 'Redis', 'Kafka'))) {
        return $false
    }

    return (Initialize-MySqlAccounts)
}

function Start-BackendServices {
    param([switch]$WaitForInfrastructure)

    Set-Location $PSScriptRoot\..

    Stop-MeditrackJavaProcesses

    if ($WaitForInfrastructure) {
        Write-Host "`nWaiting for Docker infrastructure to become ready..." -ForegroundColor Yellow

        if (-not (Wait-ForInfrastructureServices)) {
            return $false
        }

        if (-not (Initialize-MySqlAccounts)) {
            return $false
        }
    }

    Write-Host "`nStarting Spring Boot Microservices..." -ForegroundColor Cyan

    $startedServices = @()

    foreach ($service in $backendServices) {
        if (Test-ServicePort -port $service.Port) {
            Write-Host "$($service.Name) is already running on port $($service.Port). Skipping start." -ForegroundColor DarkYellow
            continue
        }

        Write-Host "`nStarting $($service.Name)..." -ForegroundColor Cyan

        if (-not (Test-Path $service.Path)) {
            Write-Host "Directory $($service.Path) not found. Skipping..." -ForegroundColor Yellow
            continue
        }

        $jarName = "$(Split-Path $service.Path -Leaf)-1.0.0.jar"
        $javaArgs = $service.JavaArgs
        if (-not $javaArgs) {
            $javaArgs = @("-Xms64m", "-Xmx256m", "-XX:+UseG1GC")
        }

        $process = Start-Process -FilePath "java" -ArgumentList @($javaArgs + @("-jar", "target/$jarName")) -WorkingDirectory $service.Path -PassThru -WindowStyle Hidden
        $startedServices += [PSCustomObject]@{
            Name    = $service.Name
            Port    = $service.Port
            Process = $process
        }

        Start-Sleep 3
    }

    if ($startedServices.Count -eq 0) {
        Write-Host "All backend services are already running." -ForegroundColor Green
        return $true
    }

    Write-Host "`nWaiting for Spring Boot services to become ready..." -ForegroundColor Yellow

    $allReady = $true
    foreach ($service in $startedServices) {
        $timeout = if ($service.Port -eq 8761 -or $service.Port -eq 8090) { 120 } else { 90 }

        if (Wait-ForService -serviceName $service.Name -port $service.Port -timeout $timeout) {
            Write-Host "$($service.Name) started successfully" -ForegroundColor Green
        }
        else {
            Write-Host "$($service.Name) failed to start" -ForegroundColor Red
            $allReady = $false
        }
    }

    return $allReady
}

function Show-ServiceUrls {
    Write-Host "`nService URLs:" -ForegroundColor Cyan
    Write-Host "Grafana Dashboard: http://localhost:3000 (admin/admin123)" -ForegroundColor White
    Write-Host "Prometheus Metrics: http://localhost:9090" -ForegroundColor White
    Write-Host "Loki Logs: http://localhost:3100" -ForegroundColor White
    Write-Host "Zipkin Tracing: http://localhost:9411" -ForegroundColor White
    Write-Host "Kafka UI: http://localhost:8095" -ForegroundColor White
    Write-Host "Eureka Server: http://localhost:8761 (admin/admin123)" -ForegroundColor White
    Write-Host "API Gateway: http://localhost:8090" -ForegroundColor White
    Write-Host "Patient Service: http://localhost:8082" -ForegroundColor White
    Write-Host "Vitals Service: http://localhost:8083" -ForegroundColor White
    Write-Host "Report Service: http://localhost:8087" -ForegroundColor White
}

switch ($Mode) {
    'DockerOnly' {
        Write-Host "`nStarting Docker Infrastructure Services..." -ForegroundColor Cyan

        if (-not (Start-DockerInfrastructure)) {
            exit 1
        }

        Write-Host "`nDocker infrastructure startup complete!" -ForegroundColor Green
    }
    'BackendOnly' {
        Write-Host "`nBackendOnly mode selected. Waiting for Docker infrastructure started by run-docker.bat..." -ForegroundColor Cyan

        if (-not (Start-BackendServices -WaitForInfrastructure)) {
            exit 1
        }

        Show-ServiceUrls
        Write-Host "`nMediTrack AI services startup complete!" -ForegroundColor Green
        Write-Host "Tip: Check individual service logs in their respective directories" -ForegroundColor Yellow
    }
    default {
        Write-Host "`nStarting Docker Infrastructure Services..." -ForegroundColor Cyan

        if (-not (Start-DockerInfrastructure)) {
            exit 1
        }

        if (-not (Start-BackendServices)) {
            exit 1
        }

        Show-ServiceUrls
        Write-Host "`nMediTrack AI services startup complete!" -ForegroundColor Green
        Write-Host "Tip: Check individual service logs in their respective directories" -ForegroundColor Yellow
    }
}
