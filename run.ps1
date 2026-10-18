# Dining Philosophers Simulator - One-Click Launcher
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Dining Philosophers Simulator (Spring Boot + Java 17)   " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan

# Check if target data directory exists
if (-not (Test-Path -Path "./data")) {
    New-Item -ItemType Directory -Path "./data" | Out-Null
}

# Check if port 8080 is already occupied
$port = 8080
$occupied = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($occupied) {
    Write-Host "[WARNING] Port 8080 is currently in use by PID $($occupied.OwningProcess[0])." -ForegroundColor Yellow
    Write-Host "[INFO] Switching server port to 8085 to avoid conflicts..." -ForegroundColor Cyan
    $port = 8085
}

Write-Host "[INFO] Launching Simulator on port $port..." -ForegroundColor Green
Write-Host "[INFO] Once started, open your browser at: http://localhost:$port" -ForegroundColor Cyan
Write-Host "----------------------------------------------------------" -ForegroundColor Gray

mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=$port"
