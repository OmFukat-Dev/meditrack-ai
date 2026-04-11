# MediTrack AI - Services Stop Script
# This script stops all services and cleans up

Write-Host "🛑 Stopping MediTrack AI Services..." -ForegroundColor Red

# Function to stop Java processes
function Stop-JavaServices {
    Write-Host "`n🏗️ Stopping Spring Boot Services..." -ForegroundColor Yellow
    
    # Get all Java processes running our services
    $javaProcesses = Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object { 
        $_.MainWindowTitle -like "*meditrack*" -or 
        $_.CommandLine -like "*meditrack*" -or
        $_.CommandLine -like "*eureka*" -or
        $_.CommandLine -like "*gateway*" -or
        $_.CommandLine -like "*patient*" -or
        $_.CommandLine -like "*vitals*" -or
        $_.CommandLine -like "*prediction*" -or
        $_.CommandLine -like "*alert*" -or
        $_.CommandLine -like "*notification*" -or
        $_.CommandLine -like "*simulator*" -or
        $_.CommandLine -like "*report*"
    }
    
    if ($javaProcesses) {
        foreach ($process in $javaProcesses) {
            Write-Host "🔄 Stopping process $($process.Id)..." -ForegroundColor Yellow
            $process.Kill()
        }
        Write-Host "✅ All Spring Boot services stopped" -ForegroundColor Green
    } else {
        Write-Host "ℹ️ No Spring Boot services found running" -ForegroundColor Cyan
    }
}

# Function to stop Docker services
function Stop-DockerServices {
    Write-Host "`n📦 Stopping Docker Infrastructure Services..." -ForegroundColor Yellow
    
    Set-Location $PSScriptRoot\..
    
    try {
        docker-compose down
        Write-Host "✅ Docker services stopped" -ForegroundColor Green
    } catch {
        Write-Host "❌ Error stopping Docker services: $_" -ForegroundColor Red
    }
}

# Function to clean up Docker resources
function Cleanup-DockerResources {
    Write-Host "`n🧹 Cleaning up Docker resources..." -ForegroundColor Yellow
    
    try {
        # Remove stopped containers
        docker container prune -f | Out-Null
        
        # Remove unused images
        docker image prune -f | Out-Null
        
        # Remove unused networks
        docker network prune -f | Out-Null
        
        Write-Host "✅ Docker cleanup completed" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ Docker cleanup failed (this is normal if no resources to clean): $_" -ForegroundColor Yellow
    }
}

# Main execution
try {
    # Stop Java services first
    Stop-JavaServices
    
    # Stop Docker services
    Stop-DockerServices
    
    # Optional: Ask for cleanup
    $cleanup = Read-Host "`n🧹 Do you want to clean up Docker resources? (y/N)"
    if ($cleanup -eq 'y' -or $cleanup -eq 'Y') {
        Cleanup-DockerResources
    }
    
    Write-Host "`n🎉 All MediTrack AI services stopped successfully!" -ForegroundColor Green
    Write-Host "💡 Tip: Use 'docker-compose up -d' to restart infrastructure services" -ForegroundColor Cyan
    
} catch {
    Write-Host "❌ Error during shutdown: $_" -ForegroundColor Red
    exit 1
}
