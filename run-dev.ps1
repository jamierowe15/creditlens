# Starts CreditLens for day-to-day development (after Setup-CreditLens.ps1 has run once).
# Usage: right-click > Run with PowerShell, or:  powershell -ExecutionPolicy Bypass -File .\run-dev.ps1
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

# .local-env.ps1 is written by the setup script: it points PATH/JAVA_HOME at the JDK, Node and Maven it used.
$envFile = Join-Path $root '.local-env.ps1'
if (Test-Path $envFile) { . $envFile }

$mvn = if ($env:CREDITLENS_MVN) { $env:CREDITLENS_MVN } else { 'mvn.cmd' }

Write-Host 'Starting backend on http://localhost:8080 ...' -ForegroundColor Cyan
Start-Process powershell -ArgumentList '-NoExit', '-ExecutionPolicy', 'Bypass', '-Command', "`$host.UI.RawUI.WindowTitle='CreditLens API'; Set-Location '$root\backend'; & '$mvn' spring-boot:run"

Write-Host 'Starting frontend on http://localhost:4200 ...' -ForegroundColor Cyan
Start-Process powershell -ArgumentList '-NoExit', '-ExecutionPolicy', 'Bypass', '-Command', "`$host.UI.RawUI.WindowTitle='CreditLens Web'; Set-Location '$root\frontend'; npm.cmd start"

Write-Host 'Waiting for the API to report healthy (first start can take a minute)...'
$deadline = (Get-Date).AddMinutes(4)
$up = $false
while (-not $up -and (Get-Date) -lt $deadline) {
    try {
        $h = Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health' -TimeoutSec 3
        $up = ($h.status -eq 'UP')
    } catch { Start-Sleep -Seconds 3 }
}
$deadline = (Get-Date).AddMinutes(3)
$webUp = $false
while (-not $webUp -and (Get-Date) -lt $deadline) {
    try {
        Invoke-WebRequest -Uri 'http://localhost:4200' -UseBasicParsing -TimeoutSec 3 | Out-Null
        $webUp = $true
    } catch { Start-Sleep -Seconds 3 }
}
Start-Process 'http://localhost:4200'
Start-Process 'http://localhost:8080/swagger-ui.html'
Write-Host 'CreditLens is running. Close the two PowerShell windows to stop it.' -ForegroundColor Green
