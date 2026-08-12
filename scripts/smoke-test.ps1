$urls = @(
    'http://localhost:8761/actuator/health',
    'http://localhost:8090/actuator/health',
    'http://localhost:8081/actuator/health',
    'http://localhost:8082/actuator/health',
    'http://localhost:8083/actuator/health',
    'http://localhost:8084/actuator/health',
    'http://localhost:8085/actuator/health',
    'http://localhost:8086/actuator/health',
    'http://localhost:8087/actuator/health',
    'http://localhost:8088/actuator/health'
)

foreach ($u in $urls) {
    $ok = $false
    $attempts = 0
    while (-not $ok -and $attempts -lt 30) {
        try {
            $r = Invoke-RestMethod -Uri $u -UseBasicParsing -TimeoutSec 5
            try {
                $json = $r | ConvertTo-Json -Compress -Depth 5
            } catch {
                $json = $r.ToString()
            }
            Write-Output "$u : UP -> $json"
            $ok = $true
        } catch {
            Start-Sleep -Seconds 2
            $attempts++
        }
    }
    if (-not $ok) { Write-Output "$u : DOWN" }
}
