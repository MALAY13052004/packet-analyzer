Set-Location (Join-Path $PSScriptRoot "..")
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { Write-Host "Java 21+ is required."; Read-Host "Press Enter"; exit 1 }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { Write-Host "Maven is required to build PacketLab."; Read-Host "Press Enter"; exit 1 }
mvn spring-boot:run
