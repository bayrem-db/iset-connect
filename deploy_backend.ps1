<#
.SYNOPSIS
Copies the PHP REST API from this project into XAMPP's htdocs folder.
Run once after cloning or after any backend change (as Administrator if needed).
#>
param(
    [string]$XamppPath = "C:\xampp"
)

$source = Join-Path $PSScriptRoot "backend"
$destination = Join-Path $XamppPath "htdocs\iset_connect\backend"

if (-not (Test-Path $source)) { throw "Backend source not found: $source" }
if (-not (Test-Path $destination)) { New-Item -ItemType Directory -Force -Path $destination | Out-Null }

Get-ChildItem -Path $destination -Recurse -File | Remove-Item -Force
Copy-Item -Path (Join-Path $source "*") -Destination $destination -Recurse -Force

Write-Host "Backend deployed to: $destination"
Write-Host "API root: http://localhost/iset_connect/backend/index.php"
Write-Host "Android emulator URL: http://10.0.2.2/iset_connect/backend/api/"
