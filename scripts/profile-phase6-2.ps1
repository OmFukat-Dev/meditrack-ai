# MediTrack AI - Phase 6.2 CPU and Memory Profiling
# Captures JFR, heap, and class histogram data from running services.

[CmdletBinding()]
param(
    [ValidateSet('all', 'patient', 'vitals')]
    [string]$Targets = 'all',

    [int]$WarmupIterations = 5,

    [int]$ProfileIterations = 20,

    [int]$DelayMs = 50,

    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

Set-Location $PSScriptRoot\..
$repoRoot = (Get-Location).Path
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$runDir = Join-Path $repoRoot "profiling/phase-6-2/$timestamp"
New-Item -ItemType Directory -Force -Path $runDir | Out-Null

function Resolve-JavaTool {
    param([string]$ToolName)

    $command = Get-Command $ToolName -ErrorAction SilentlyContinue
    if ($command -and $command.Source) {
        return $command.Source
    }

    $javaCommand = Get-Command java -ErrorAction Stop
    $javaDir = Split-Path -Parent $javaCommand.Source
    $candidate = Join-Path $javaDir ($ToolName + '.exe')

    if (Test-Path $candidate) {
        return $candidate
    }

    throw "Unable to resolve $ToolName. Make sure the JDK bin directory is on PATH."
}

function Get-ListeningPid {
    param([int]$Port)

    $connection = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
        Select-Object -First 1

    if (-not $connection) {
        throw "No listening process found on port $Port. Start the service first."
    }

    return $connection.OwningProcess
}

function Start-JfrRecording {
    param(
        [string]$JcmdPath,
        [int]$Pid,
        [string]$RecordingName
    )

    & $JcmdPath $Pid "JFR.start name=$RecordingName settings=profile delay=0s disk=true" | Out-Null
}

function Stop-JfrRecording {
    param(
        [string]$JcmdPath,
        [int]$Pid,
        [string]$RecordingName,
        [string]$OutputFile
    )

    & $JcmdPath $Pid "JFR.dump name=$RecordingName filename=$OutputFile" | Out-Null
    & $JcmdPath $Pid "JFR.stop name=$RecordingName" | Out-Null
}

function Capture-JvmStats {
    param(
        [string]$JcmdPath,
        [int]$Pid,
        [string]$Prefix
    )

    & $JcmdPath $Pid "GC.heap_info" 2>&1 | Out-File -Encoding utf8 (Join-Path $runDir "$Prefix-heap.txt")
    & $JcmdPath $Pid "GC.class_histogram" 2>&1 | Out-File -Encoding utf8 (Join-Path $runDir "$Prefix-class-histogram.txt")

    try {
        & $JcmdPath $Pid "VM.native_memory summary" 2>&1 | Out-File -Encoding utf8 (Join-Path $runDir "$Prefix-native-memory.txt")
    } catch {
        Set-Content -Encoding utf8 -Path (Join-Path $runDir "$Prefix-native-memory.txt") -Value "Native memory tracking is not enabled for this process."
    }
}

function Invoke-Workload {
    param(
        [string]$BaseUrl,
        [string[]]$Paths,
        [int]$Iterations
    )

    $client = [System.Net.Http.HttpClient]::new()
    $client.Timeout = [TimeSpan]::FromSeconds(20)
    $results = New-Object System.Collections.Generic.List[object]

    try {
        for ($iteration = 0; $iteration -lt $Iterations; $iteration++) {
            foreach ($path in $Paths) {
                $uri = $BaseUrl.TrimEnd('/') + $path
                $sw = [System.Diagnostics.Stopwatch]::StartNew()
                $statusCode = -1

                try {
                    $response = $client.GetAsync($uri).GetAwaiter().GetResult()
                    $statusCode = [int]$response.StatusCode
                    $null = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
                    $response.Dispose()
                } catch {
                    $statusCode = -1
                } finally {
                    $sw.Stop()
                    $results.Add([pscustomobject]@{
                        Path       = $path
                        StatusCode = $statusCode
                        DurationMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
                    })
                }

                if ($DelayMs -gt 0) {
                    Start-Sleep -Milliseconds $DelayMs
                }
            }
        }
    } finally {
        $client.Dispose()
    }

    return $results
}

function Write-Summary {
    param(
        [string]$Name,
        [object[]]$Measurements,
        [string]$OutputPath
    )

    $summary = $Measurements |
        Group-Object Path |
        ForEach-Object {
            $durations = $_.Group | Select-Object -ExpandProperty DurationMs
            [pscustomobject]@{
                Service        = $Name
                Path           = $_.Name
                Requests       = $_.Count
                AvgMs          = [math]::Round((($durations | Measure-Object -Average).Average), 2)
                MinMs          = [math]::Round((($durations | Measure-Object -Minimum).Minimum), 2)
                MaxMs          = [math]::Round((($durations | Measure-Object -Maximum).Maximum), 2)
                ErrorResponses = ($_.Group | Where-Object { $_.StatusCode -ge 400 -or $_.StatusCode -lt 0 }).Count
            }
        } |
        Sort-Object Path

    $summary | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $runDir "$OutputPath.json")
    $summary | Format-Table -AutoSize | Out-String | Set-Content -Encoding utf8 (Join-Path $runDir "$OutputPath.txt")
}

$patientProfile = @{
    Name      = 'patient-service'
    Port      = 8082
    BaseUrl   = 'http://localhost:8082'
    Recording = 'phase62-patient'
    JfrFile   = Join-Path $runDir 'patient-service.jfr'
    Output    = 'patient-service'
    Paths     = @(
        '/api/patients?page=0&size=50',
        '/api/patients/search?query=a&page=0&size=50',
        '/api/patients/filter/gender/FEMALE?page=0&size=50',
        '/api/fhir/patients?page=0&size=50',
        '/api/fhir/patients/search?name=a&page=0&size=50',
        '/api/patients/statistics/total-active'
    )
}

$vitalsProfile = @{
    Name      = 'vitals-service'
    Port      = 8083
    BaseUrl   = 'http://localhost:8083'
    Recording = 'phase62-vitals'
    JfrFile   = Join-Path $runDir 'vitals-service.jfr'
    Output    = 'vitals-service'
    Paths     = @(
        '/api/vitals/health',
        '/api/vitals/stats',
        '/api/vitals/patient/1?page=0&size=25',
        '/api/vitals/patient/1/latest',
        '/api/vitals/patient/1/abnormal',
        '/api/vitals/patient/1/critical',
        '/api/vitals/patient/1/summary'
    )
}

$profiles = @()
switch ($Targets) {
    'patient' { $profiles = @($patientProfile) }
    'vitals'  { $profiles = @($vitalsProfile) }
    default   { $profiles = @($patientProfile, $vitalsProfile) }
}

$javaPath = Resolve-JavaTool -ToolName 'java'
$jcmdPath = Resolve-JavaTool -ToolName 'jcmd'

if ($DryRun) {
    Write-Host "Dry run mode enabled." -ForegroundColor Yellow
    Write-Host "Java: $javaPath"
    Write-Host "JCmd: $jcmdPath"
    Write-Host "Output directory: $runDir"

    foreach ($profile in $profiles) {
        Write-Host "Target: $($profile.Name) on port $($profile.Port)"
    }

    exit 0
}

$report = New-Object System.Collections.Generic.List[object]

foreach ($profile in $profiles) {
    $pid = Get-ListeningPid -Port $profile.Port
    Write-Host "Profiling $($profile.Name) (PID $pid)..." -ForegroundColor Cyan

    $warmup = Invoke-Workload -BaseUrl $profile.BaseUrl -Paths $profile.Paths -Iterations $WarmupIterations
    Start-JfrRecording -JcmdPath $jcmdPath -Pid $pid -RecordingName $profile.Recording
    $measurements = Invoke-Workload -BaseUrl $profile.BaseUrl -Paths $profile.Paths -Iterations $ProfileIterations
    Stop-JfrRecording -JcmdPath $jcmdPath -Pid $pid -RecordingName $profile.Recording -OutputFile $profile.JfrFile
    Capture-JvmStats -JcmdPath $jcmdPath -Pid $pid -Prefix $profile.Output

    $allMeasurements = @($warmup) + @($measurements)
    Write-Summary -Name $profile.Name -Measurements $allMeasurements -OutputPath $profile.Output

    $report.Add([pscustomobject]@{
        Service          = $profile.Name
        Pid              = $pid
        JfrFile          = $profile.JfrFile
        SummaryFile      = Join-Path $runDir "$($profile.Output).txt"
        HeapFile         = Join-Path $runDir "$($profile.Output)-heap.txt"
        HistogramFile    = Join-Path $runDir "$($profile.Output)-class-histogram.txt"
        NativeMemoryFile = Join-Path $runDir "$($profile.Output)-native-memory.txt"
    })
}

$reportPath = Join-Path $runDir 'profile-manifest.json'
$report | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 $reportPath

Write-Host ""
Write-Host "Phase 6.2 profiling complete." -ForegroundColor Green
Write-Host "Artifacts written to: $runDir" -ForegroundColor Green
Write-Host "Manifest: $reportPath" -ForegroundColor Green
