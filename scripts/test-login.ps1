# Test login endpoint
$uri = 'http://localhost:8090/api/auth/login'
$body = @{
    email = 'om@meditrackadmin.ai'
    password = 'admin123'
} | ConvertTo-Json

Write-Output "Testing login endpoint: $uri"
Write-Output "Payload: $body"
Write-Output ""

try {
    $response = Invoke-WebRequest -Uri $uri -Method POST -Body $body -ContentType 'application/json' -TimeoutSec 5 -ErrorAction Stop
    Write-Output "✅ Login SUCCESS"
    Write-Output "Status Code: $($response.StatusCode)"
    Write-Output "Response:"
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5
} catch {
    Write-Output "❌ Login FAILED"
    if ($_.Exception.Response) {
        Write-Output "Status Code: $($_.Exception.Response.StatusCode)"
        $errorContent = $_.Exception.Response.Content
        if ($errorContent) {
            Write-Output "Error Response:"
            try {
                $errorContent | ConvertFrom-Json | ConvertTo-Json -Depth 5
            } catch {
                Write-Output $errorContent
            }
        }
    } else {
        Write-Output "Error: $($_.Exception.Message)"
    }
}
